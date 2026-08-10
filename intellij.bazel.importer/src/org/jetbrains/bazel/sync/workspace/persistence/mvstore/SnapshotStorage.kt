package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import com.esotericsoftware.kryo.kryo5.Kryo
import com.esotericsoftware.kryo.kryo5.io.ByteBufferInput
import com.esotericsoftware.kryo.kryo5.io.Output
import com.esotericsoftware.kryo.kryo5.util.Pool
import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.io.mvstore.openOrResetMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.longs.Long2ObjectMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.h2.mvstore.MVMap
import org.h2.mvstore.type.DataType
import org.h2.mvstore.type.IntegerDataType
import org.h2.mvstore.type.LongDataType
import org.h2.mvstore.type.StringDataType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.persistence.BuildTargetLoadHint
import org.jetbrains.bazel.sync.workspace.persistence.TargetLoadOptions
import org.jetbrains.bazel.sync.workspace.persistence.TargetSection
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.SourceFileCollection
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.file.Path
import java.util.Arrays
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

private const val INITIAL_VALUE_BUFFER_SIZE = 8 * 1024
private const val ENCODE_CHUNK_SIZE = 256

private val EMPTY_FILE_SETS = HeavyWorkspaceTarget(
  sources = SourceFileCollection.EMPTY,
  generatedSources = SourceFileCollection.EMPTY,
  resources = SourceFileCollection.EMPTY,
)

// limit frame encoder threads to avoid lock contention on kryo instance pool
private val ENCODE_DISPATCHER = Dispatchers.IO.limitedParallelism(KRYO_POOL_INSTANCES)

private val KRYO_OUTPUT_POOL: Pool<Output> =
  object : Pool<Output>(/* threadSafe = */ true, /* softReferences = */ false, /* maximumCapacity = */ 16) {
    override fun create(): Output = Output(INITIAL_VALUE_BUFFER_SIZE, -1)
  }

/**
 * Storage is split into [SnapshotGeneration], each generation is set of MVStore maps
 * that together are used to reconstruct original [BuildTarget] based on loading conditions [TargetLoadOptions],
 *
 * We must always keep previous snapshot generation in order to have fallback in case of update failure/cancellation,
 * this might cause storage file on disk be 2x larger than actual data, but the good news is that is barely impact read/write
 * performance - only larger file on disk.
 *
 * Storage maps are keyed by stable target integer ID, those IDs are calculated on each storage save from scratch
 * and saved to blob metadata, that allow for relatively fast traversals and insertions.
 * Used maps work in "single writer" mode, that means we can access underlying B-Tree in faster way using [MVMap.append],
 * also for even bigger speedup we're always sorting keys before insertion to allow MVStore to predictibly write pages.
 *
 * Storage uses [ValueFrame] to store actual object payloads,
 * this allows for simple copies of map values (very important for future incremental storage updates),
 * also is means easy parallel deserialization/serialization of underlying data which again improve write/read
 * speeds - most impact for current monolithic updates.
 *
 * Some custom Kryo serializers use global string table (only for selected types) to avoid duplication enormous amount of data,
 * for ultimate case it caused near double reduction ~110mb -> ~60mb in on disk database size, and overall read/write speed improvements
 * for full materializations.
 */
@ApiStatus.Internal
class SnapshotStorage(
  private val dbFile: Path,
  internal val kryo: SnapshotKryo,
) {
  companion object {
    private val log = logger<SnapshotStorage>()

    private fun mapName(generation: Int, suffix: String) = "snapshot.g$generation.$suffix"
  }

  private val store = createOrResetMvStore(log, dbFile)
  private val openedMaps = ConcurrentHashMap<String, MVMap<*, *>>()
  private val generationRefMap = ConcurrentHashMap<Int, MutableList<WeakReference<SnapshotGeneration>>>()

  internal val allBuildTargetIds: IntList = kryo.universe.registrations
    .filter { !it.type.isInterface && BuildTargetData::class.java.isAssignableFrom(it.type) }
    .map { it.registrationId }
    .sorted()
    .let { IntArrayList(it) }

  val isClosed: Boolean
    get() = store.isClosed

  fun hasGeneration(generation: Int): Boolean = store.hasMap(mapName(generation, "id2PartialTarget"))

  fun openGeneration(generation: Int): SnapshotGeneration {
    val handle =  SnapshotGeneration(
      generation = generation,
      storage = this,
      id2PartialTarget = openFrameMap(mapName(generation, "id2PartialTarget"), IntegerDataType.INSTANCE),
      id2TargetDeps = openFrameMap(mapName(generation, "id2TargetDeps"), IntegerDataType.INSTANCE),
      id2HeavyTarget = openFrameMap(mapName(generation, "id2HeavyTarget"), IntegerDataType.INSTANCE),
      id2TargetData = openFrameMap(mapName(generation, "id2TargetData"), LongDataType.INSTANCE),
      file2KeyIds = openMap(mapName(generation, "file2KeyIds"), LongDataType.INSTANCE, IntListDataType),
      executables = openMap(mapName(generation, "executables"), IntegerDataType.INSTANCE, IntListDataType),
      strings = openMap(mapName(generation, "strings"), IntegerDataType.INSTANCE, StringDataType.INSTANCE),
    )
    generationRefMap.computeIfAbsent(generation) { CopyOnWriteArrayList() }
      .add(WeakReference(handle))
    return handle
  }

  // openOrResetMap opens-or-creates, so createGeneration == openGeneration on names that were just swept
  fun createGeneration(generation: Int): SnapshotGeneration = openGeneration(generation)

  fun sweepGenerationsExcept(keep: Set<Int>) {
    for (name in store.mapNames.toList()) {
      if (!name.startsWith("snapshot.g")) {
        continue
      }
      val generation = name.removePrefix("snapshot.g").substringBefore('.').toIntOrNull() ?: continue
      if (generation in keep) {
        continue
      }
      if (isGenerationReferenced(generation)) {
        continue
      }
      store.removeMap(openedMaps.remove(name) ?: openUnknownMap(name))
    }
  }

  private fun isGenerationReferenced(generation: Int): Boolean {
    val handles = generationRefMap[generation] ?: return false
    handles.removeIf { it.get() == null }
    if (handles.isEmpty()) {
      generationRefMap.remove(generation, handles)
      return false
    }
    return true
  }

  private fun openUnknownMap(name: String): MVMap<ByteArray, ByteArray> {
    val builder = MVMap.Builder<ByteArray, ByteArray>()
    builder.setKeyType(UnknownDataType)
    builder.setValueType(UnknownDataType)
    builder.singleWriter()
    return store.openMap(name, builder)
  }

  fun compactFile(maxCompactTimeMillis: Int) {
    store.compactFile(maxCompactTimeMillis)
  }

  fun rollback() {
    store.rollback()
  }

  fun commit() {
    store.commit()
  }

  fun tryCommit() {
    if (store.hasUnsavedChanges()) {
      store.tryCommit()
    }
  }

  fun close() {
    store.closeImmediately()
  }

  internal fun encodeFrame(kryo: Kryo, output: Output, value: Any, stringTable: StringTableWriter? = null): ValueFrame {
    try {
      output.reset()
      if (stringTable != null) {
        // cleared by kryo's auto-reset after the top-level write
        kryo.graphContext.put(STRING_TABLE_WRITE_KEY, stringTable)
      }
      kryo.writeClassAndObject(output, value)
      return ValueFrame.Present(SNAPSHOT_FORMAT_VERSION, output.toBytes())
    }
    catch (e: Throwable) {
      log.error("Error serializing snapshot value", e)
      return ValueFrame.Error
    }
  }

  internal fun <T> decodeFrame(frame: ValueFrame?, type: Class<T>, stringTable: StringTableReader? = null): T? {
    if (frame !is ValueFrame.Present) {
      return null
    }
    if (frame.version != SNAPSHOT_FORMAT_VERSION) {
      return null
    }
    return try {
      val input = ByteBufferInput(ByteBuffer.wrap(frame.payload))
      val obj = kryo.valuePool.use { k ->
        if (stringTable != null) {
          // cleared by kryo's auto-reset after the top-level read
          k.graphContext.put(STRING_TABLE_READ_KEY, stringTable)
        }
        k.readClassAndObject(input)
      }
      type.cast(obj)
    }
    catch (e: Throwable) {
      log.error("Error deserializing snapshot value", e)
      null
    }
  }

  private fun <K> openFrameMap(name: String, keyType: DataType<K>): MVMap<K, ValueFrame> {
    val builder = MVMap.Builder<K, ValueFrame>()
    builder.setKeyType(keyType)
    builder.setValueType(createFrameDataType())
    builder.singleWriter()
    return openOrResetMap(store, name, builder) { log }
      .also { openedMaps[name] = it }
  }

  private fun <K, V> openMap(name: String, keyType: DataType<K>, valueType: DataType<V>): MVMap<K, V> {
    val builder = MVMap.Builder<K, V>()
    builder.setKeyType(keyType)
    builder.setValueType(valueType)
    builder.singleWriter()
    return openOrResetMap(store, name, builder) { log }
      .also { openedMaps[name] = it }
  }
}

@ApiStatus.Internal
class SnapshotGeneration internal constructor(
  val generation: Int,
  private val storage: SnapshotStorage,
  private val id2PartialTarget: MVMap<Int, ValueFrame>,
  private val id2TargetDeps: MVMap<Int, ValueFrame>,
  private val id2HeavyTarget: MVMap<Int, ValueFrame>,

  // key: (keyId, buildTargetId) packed into long
  private val id2TargetData: MVMap<Long, ValueFrame>,
  private val file2KeyIds: MVMap<Long, IntArrayList>,
  private val executables: MVMap<Int, IntArrayList>,
  private val strings: MVMap<Int, String>,
) {
  private val fullTargetCache: Cache<WorkspaceTargetKey, BuildTarget?> = Caffeine.newBuilder()
    .weakValues() // TODO: check if cleaning cache after sync is better
    .build()

  // global generation string table, currently used for efficient serialization
  // of paths and labels, which are very likely to duplicate across isolated targets
  // inside spread maps, due to MVStore `DataType` serialization separation (`ValueFrame`),
  // we can access this table on the fly while reading/writing objects
  private val stringTable = StringTableReader(strings)

  internal fun isLive(): Boolean = !id2PartialTarget.isClosed

  private fun composeNewWorkspaceTarget(
    partialSnapshot: PersistentWorkspaceSnapshot,
    key: WorkspaceTargetKey,
    options: TargetLoadOptions,
  ): BuildTarget? {
    val keyId = partialSnapshot.keyId2Target.getReverseOrDefault(key, -1)
    if (keyId < 0) {
      // TODO: assert
      return null
    }
    val partialData = storage.decodeFrame(id2PartialTarget[keyId], PartialWorkspaceTarget::class.java, stringTable)
                      ?: return null
    return composeFromFrames(
      key = key,
      options = options,
      partialData = partialData,
      depsFrame = { frameOf(id2TargetDeps, keyId) },
      heavyFrame = { frameOf(id2HeavyTarget, keyId) },
      dataFrameForType = { typeId -> frameOf(id2TargetData, BuildTargetPack.pack(keyId, typeId)) },
    )
  }

  private inline fun composeFromFrames(
    key: WorkspaceTargetKey,
    options: TargetLoadOptions,
    partialData: PartialWorkspaceTarget,
    crossinline depsFrame: () -> ValueFrame?,
    crossinline heavyFrame: () -> ValueFrame?,
    // invoked in ascending typeId order (both id sources below are sorted), so the bulk scan may
    // back this with a forward-only cursor over the packed-key map
    crossinline dataFrameForType: (typeId: Int) -> ValueFrame?,
  ): BuildTarget {
    val depsSection = LazyTargetSection(TargetSection.DEPS in options.sections) { decodeDeps(depsFrame()) }
    val fileSetsSection = LazyTargetSection(TargetSection.FILE_SETS in options.sections) { decodeFileSets(heavyFrame()) }
    val dataTypeIds = when (val hint = options.targetData) {
      is BuildTargetLoadHint.Subset -> typeIdsOf(hint.data)
      BuildTargetLoadHint.All, BuildTargetLoadHint.None -> storage.allBuildTargetIds
    }
    val dataSection = LazyTargetSection(options.targetData != BuildTargetLoadHint.None) {
      decodeData(dataTypeIds, dataFrameForType)
    }
    return LazyWorkspaceTarget(
      key = key,
      kind = partialData.kind,
      baseDirectory = partialData.baseDirectory,
      generatorName = partialData.generatorName,
      isManual = partialData.isManual,
      isWorkspace = partialData.isWorkspace,
      isTestOnly = partialData.isTestOnly,
      tags = partialData.tags,
      loaded = options.copy(sections = options.sections + TargetSection.INFO),
      depsSection = depsSection,
      fileSetsSection = fileSetsSection,
      dataSection = dataSection,
    )
  }

  private fun decodeDeps(frame: ValueFrame?): List<DependencyLabel> =
    storage.decodeFrame(frame, WorkspaceTargetDeps::class.java, stringTable)?.dependencies ?: listOf()

  private fun decodeFileSets(frame: ValueFrame?): HeavyWorkspaceTarget =
    storage.decodeFrame(frame, HeavyWorkspaceTarget::class.java, stringTable) ?: EMPTY_FILE_SETS

  private inline fun decodeData(typeIds: IntList, dataFrameForType: (typeId: Int) -> ValueFrame?): List<BuildTargetData> {
    val loadedData = ArrayList<BuildTargetData>(typeIds.size)
    for (n in typeIds.indices) {
      val frame = dataFrameForType(typeIds.getInt(n))
      loadedData += storage.decodeFrame(frame, BuildTargetData::class.java, stringTable) ?: continue
    }
    return loadedData
  }

  private fun typeIdsOf(types: Set<Class<out BuildTargetData>>): IntList {
    val typeIds = IntArrayList(types.size)
    for (type in types) {
      val typeId = storage.kryo.universe.type2Id.getOrDefault(type, -1)
      if (typeId < 0) {
        // TODO: assert
        continue
      }
      typeIds.add(typeId)
    }
    // ascending, per the dataFrameForType ordering contract
    typeIds.sort(null)
    return typeIds
  }

  // guarded frame read
  private fun <K> frameOf(map: MVMap<K, ValueFrame>, mapKey: K): ValueFrame? {
    if (!isLive()) {
      return null
    }
    return map[mapKey]
  }

  fun scanAllTargets(partialSnapshot: PersistentWorkspaceSnapshot, options: TargetLoadOptions): Sequence<BuildTarget> = sequence {
    if (!isLive()) {
      return@sequence
    }
    val skipDeps = TargetSection.DEPS !in options.sections
    val skipFileSets = TargetSection.FILE_SETS !in options.sections
    val skipData = options.targetData == BuildTargetLoadHint.None
    val depsCursor = if (skipDeps) null else PeekingEntryCursor(id2TargetDeps) { it.toLong() }
    val heavyCursor = if (skipFileSets) null else PeekingEntryCursor(id2HeavyTarget) { it.toLong() }
    val dataCursor = if (skipData) null else PeekingEntryCursor(id2TargetData) { it }

    for ((keyId, partialFrame) in id2PartialTarget.entries) {
      val key = partialSnapshot.keyId2Target.get(keyId) ?: continue
      val partialData = storage.decodeFrame(partialFrame, PartialWorkspaceTarget::class.java, stringTable) ?: continue

      val depsFrame = depsCursor?.advanceTo(keyId.toLong())
      val heavyFrame = heavyCursor?.advanceTo(keyId.toLong())

      // composeFromFrames requests typeIds ascending and keyIds ascend across iterations, so the
      // packed keys form one ascending pass over the data map, stale rows of skipped keyIds are
      // discarded raw by the next advanceTo
      val target = composeFromFrames(
        key = key,
        options = options,
        partialData = partialData,
        depsFrame = { depsFrame ?: frameOf(id2TargetDeps, keyId) },
        heavyFrame = { heavyFrame ?: frameOf(id2HeavyTarget, keyId) },
      ) { typeId ->
        val packedKey = BuildTargetPack.pack(keyId, typeId)
        if (dataCursor == null) {
          frameOf(id2TargetData, packedKey)
        }
        else {
          dataCursor.advanceTo(packedKey)
        }
      }
      yield(target)
    }
  }

  fun findOrLoadTarget(partialSnapshot: PersistentWorkspaceSnapshot, key: WorkspaceTargetKey, options: TargetLoadOptions): BuildTarget? {
    if (!isLive()) {
      return null
    }
    // cached full target satisfies any request
    fullTargetCache.getIfPresent(key)?.let { return it }
    if (options == TargetLoadOptions.ALL) {
      return fullTargetCache.get(key) { composeNewWorkspaceTarget(partialSnapshot, it, TargetLoadOptions.ALL) }
    }
    // partial loads are the memory-light bulk path: composed fresh, never cached
    return composeNewWorkspaceTarget(partialSnapshot, key, options)
  }

  // TODO: assert not partial
  @VisibleForTesting
  fun saveTarget(target: WorkspaceTargetToSave) {
    val encoded = storage.kryo.valuePool.use { kryo -> KRYO_OUTPUT_POOL.use { output -> encodeTarget(target, kryo, output) } }
    appendTarget(encoded)
  }

  suspend fun saveTargets(targets: List<WorkspaceTargetToSave>) {
    val stringTable = StringTableWriter()
    val encodedChunks = withContext(ENCODE_DISPATCHER) {
      targets.chunked(ENCODE_CHUNK_SIZE)
        .map { chunk ->
          async {
            // use single kryo for batch to avoid pool lock contention
            storage.kryo.valuePool.use { kryo ->
              KRYO_OUTPUT_POOL.use { output ->
                chunk.map { encodeTarget(it, kryo, output, stringTable) }
              }
            }
          }
        }
        .awaitAll()
    }
    for (chunk in encodedChunks) {
      for (target in chunk) {
        appendTarget(target)
      }
    }
    // persisted in the same commit as the frames that reference it
    for ((id, value) in stringTable.sortedEntries()) {
      strings.append(id, value)
    }

    fullTargetCache.invalidateAll()
  }

  private fun encodeTarget(
    target: WorkspaceTargetToSave,
    kryo: Kryo,
    output: Output,
    stringTable: StringTableWriter? = null,
  ): EncodedWorkspaceTarget {
    val raw = target.target

    val partialTarget = PartialWorkspaceTarget(
      kind = raw.kind,
      baseDirectory = raw.baseDirectory,
      generatorName = raw.generatorName,
      isManual = raw.isManual,
      isWorkspace = raw.isWorkspace,
      isTestOnly = raw.isTestOnly,
      tags = raw.tags,
    )
    val targetDeps = WorkspaceTargetDeps(dependencies = raw.dependencies)
    val heavyTarget = HeavyWorkspaceTarget(
      sources = raw.sources,
      generatedSources = raw.generatedSources,
      resources = raw.resources,
    )

    // packed keys must stay ascending for append, sort by data id within the target
    val sortedData = raw.data.map { data ->
      val dataId = storage.kryo.universe.type2Id.getOrDefault(data.javaClass, -1)
      if (dataId < 0) {
        error("Unrecognized `${BuildTargetData::class}` type `${data::class}`")
      }
      else {
        dataId to data
      }
    }.sortedBy { it.first }

    return EncodedWorkspaceTarget(
      keyId = target.keyId,
      partialFrame = storage.encodeFrame(kryo, output, partialTarget, stringTable),
      depsFrame = storage.encodeFrame(kryo, output, targetDeps, stringTable),
      heavyFrame = storage.encodeFrame(kryo, output, heavyTarget, stringTable),
      dataFrames = sortedData.map { (dataId, data) -> dataId to storage.encodeFrame(kryo, output, data, stringTable) },
    )
  }

  // the single-writer: appends only, must stay on one thread
  private fun appendTarget(encoded: EncodedWorkspaceTarget) {
    val keyId = encoded.keyId
    id2PartialTarget.append(keyId, encoded.partialFrame)
    id2TargetDeps.append(keyId, encoded.depsFrame)
    id2HeavyTarget.append(keyId, encoded.heavyFrame)
    for ((dataId, frame) in encoded.dataFrames) {
      id2TargetData.append(BuildTargetPack.pack(keyId, dataId), frame)
    }
  }

  // <path hash> -> <list of key IDs>
  fun saveFileToTargetMap(map: Long2ObjectMap<IntArrayList>) {
    val keys = map.keys.toLongArray()
    Arrays.sort(keys)
    for (hash in keys) {
      val keyIds = map.get(hash) ?: continue
      file2KeyIds.append(hash, keyIds)
    }
  }

  fun findTargetsByFile(partialSnapshot: PersistentWorkspaceSnapshot, file: Path): Sequence<WorkspaceTargetKey> {
    if (!isLive()) {
      return emptySequence()
    }
    return (file2KeyIds[hashFilePath(file)] ?: return emptySequence())
      .asSequence()
      .mapNotNull { partialSnapshot.keyId2Target.get(it) }
  }

  fun putFileMapping(hash: Long, keyIds: IntArrayList) {
    if (!isLive()) {
      return
    }
    // in contrast, we're using put() here
    file2KeyIds[hash] = keyIds
  }

  fun removeFileMapping(hash: Long) {
    if (!isLive()) {
      return
    }
    file2KeyIds.remove(hash)
  }

  fun fileCount(): Int = if (isLive()) file2KeyIds.size else 0

  // <label id> -> <list of label id>
  fun saveExecutableTargets(map: Int2ObjectMap<IntArrayList>) {
    val keys = map.keys.toIntArray()
    Arrays.sort(keys)
    for (labelId in keys) {
      val keyIds = map.get(labelId) ?: continue
      executables.append(labelId, keyIds)
    }
  }

  fun findExecutableTargets(partialSnapshot: PersistentWorkspaceSnapshot, label: Label): List<Label> {
    if (!isLive()) {
      return listOf()
    }
    val labelId = partialSnapshot.labelId2Label.getReverseOrDefault(label, -1)
    if (labelId < 0) {
      return listOf()
    }
    val executables = executables[labelId] ?: return listOf()
    val executableLabels = ArrayList<Label>(executables.size)
    for (n in executables.indices) {
      val executableLabel = partialSnapshot.labelId2Label[executables.getInt(n)] ?: continue
      executableLabels.add(executableLabel)
    }
    return executableLabels
  }
}

private class EncodedWorkspaceTarget(
  val keyId: Int,
  val partialFrame: ValueFrame,
  val depsFrame: ValueFrame,
  val heavyFrame: ValueFrame,

  // (dataId, frame) sorted ascending by dataId
  // TODO: maybe Int2ArrayMap<...> here?
  val dataFrames: List<Pair<Int, ValueFrame>>,
)

internal object BuildTargetPack {
  fun pack(keyId: Int, buildTargetId: Int): Long = (keyId.toLong() shl 32) or (buildTargetId.toLong() and 0xFFFF_FFFFL)
}
