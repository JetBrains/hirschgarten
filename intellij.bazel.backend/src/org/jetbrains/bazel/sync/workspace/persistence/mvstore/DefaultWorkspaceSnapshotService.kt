package org.jetbrains.bazel.sync.workspace.persistence.mvstore

import com.esotericsoftware.kryo.kryo5.io.ByteBufferInput
import com.esotericsoftware.kryo.kryo5.io.ByteBufferOutput
import com.intellij.configurationStore.SettingsSavingComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotUpdater
import org.jetbrains.bazel.sync.workspace.snapshot.CommonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.ExecutableTargetsIndexBuilder
import org.jetbrains.bazel.sync.workspace.snapshot.InMemoryFileToTargetMap
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bazel.sync.workspace.snapshot.allSources
import org.jetbrains.bsp.protocol.BuildTarget
import java.nio.ByteBuffer
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists
import kotlin.io.path.moveTo
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

// TODO: implement `CachesInvalidator`

/**
 * Lazy-loadeable [WorkspaceSnapshot] storage engine.
 *
 * Storage itself is split into two parts: lightweight on disk blob storage (refered as `blob`)
 * and heavy MVStore database (refered as `database`). Blob store lightweight data that is not worth to lazy load,
 * it also serve as authority when it comes to generation selection and also carry schema manifest.
 * MVStore serve as partial storage for targets and related data, each target can be loaded depending on user needs.
 *
 * Storage engine is backed by Kryo binary serializer, optimized for minimal data duplication and overall speed.
 * It allows for easy and efficient serialization of arbitrary java/kotlin objects.
 *
 * Storage invalidation is based on closed-world assumption of serializable universe.
 * Each type that will appear inside serializable snapshot must be previously specified inside `WorkspaceTypeContributor`,
 * It allows storage engine to reason when to perform invalidation based on type universe and type definitions itself (mainly fields).
 * Besides that blob also carry global [SNAPSHOT_FORMAT_VERSION] which define compatibility
 * level of low-level kryo codecs and MVStore data types, each modification of those must be followed by [SNAPSHOT_FORMAT_VERSION] bump.
 *
 * At this point in time snapshot updates should be considered heavy and only done when absolutely necessary,
 * current implementation will schedule heavy storage update each time update is performed regardless of snapshot changes.
 * Overall goal should be to make it only update targets that actually have changes.
 */
@OptIn(FlowPreview::class)
@ApiStatus.Internal
class DefaultWorkspaceSnapshotService(
  private val project: Project,
  private val coroutineScope: CoroutineScope,
) : WorkspaceSnapshotService, SettingsSavingComponent, Disposable {

  companion object {
    private val log = logger<DefaultWorkspaceSnapshotService>()
    private const val BLOB_FILE_NAME = "snapshot_blob.data"
    private const val DB_FILE_NAME = "snapshot_db.data"

    private const val COMPACT_TIME_BUDGET_MILLIS = 5_000
  }

  private val blobFile = project.getProjectDataPath(BLOB_FILE_NAME)
  private val dbFile = project.getProjectDataPath(DB_FILE_NAME)
  private val commits = MutableStateFlow(WorkspaceSnapshot.EMPTY)
  private val kryo = SnapshotKryo(universe = SnapshotTypeUniverse.buildFromProject(project))
  private val storageLazy = lazy { SnapshotStorage(dbFile = dbFile, kryo = kryo) }
  private val storage by storageLazy
  private val updateLock = Mutex()
  private val storeLock = Mutex()

  @Volatile
  private var lastStoredSnapshot: WorkspaceSnapshot = WorkspaceSnapshot.EMPTY

  @Volatile
  private var lastStoredSource: WorkspaceSnapshot = WorkspaceSnapshot.EMPTY

  override val snapshot: StateFlow<WorkspaceSnapshot> = commits.asStateFlow()

  private val initialLoad: Job = coroutineScope.launch {
    try {
      val (loaded, time) = measureTimedValue { load() }
      log.info("`WorkspaceSnapshot` initial load time: ${time.toString(DurationUnit.MILLISECONDS, 2)}")
      if (loaded !== WorkspaceSnapshot.EMPTY) {
        lastStoredSnapshot = loaded
        lastStoredSource = loaded
        commits.compareAndSet(WorkspaceSnapshot.EMPTY, loaded)
      }
    }
    catch (e: Throwable) {
      log.error("Error loading workspace snapshot", e)
    }
  }

  init {
    coroutineScope.launch {
      commits.dropWhile { it === WorkspaceSnapshot.EMPTY }
        .debounce(1.seconds)
        .flowOn(Dispatchers.Default)
        .collect { store(it) }
    }
  }

  override suspend fun currentSnapshot(): WorkspaceSnapshot {
    initialLoad.join()
    return commits.value
  }

  override suspend fun <R> update(updater: WorkspaceSnapshotUpdater<R>): Pair<WorkspaceSnapshot, R> {
    // an early updater must not derive from EMPTY
    initialLoad.join()
    return updateLock.withLock {
      val (newSnapshot, returnValue) = updater.update(commits.value)
      commits.value = newSnapshot
      newSnapshot to returnValue
    }
  }

  override suspend fun save() {
    val pending = commits.value
    if (pending !== WorkspaceSnapshot.EMPTY) {
      store(pending)
    }
    storeLock.withLock {
      if (storageLazy.isInitialized() && !storage.isClosed) {
        (commits.value.fileToTarget as? PersistentFileToTargetMap)?.flush()
        storage.tryCommit()
      }
    }
  }

  private suspend fun load(): WorkspaceSnapshot {
    if (!blobFile.exists()) {
      return WorkspaceSnapshot.EMPTY
    }
    // TODO: can be optimized to zero-copy file channel
    val input = ByteBufferInput(ByteBuffer.wrap(blobFile.readBytes()))

    val formatVersion = input.readInt()
    if (formatVersion != SNAPSHOT_FORMAT_VERSION) {
      // delete stale database, at this point db isn't opened yet
      dbFile.deleteIfExists()
      return WorkspaceSnapshot.EMPTY
    }

    val fingerprintLength = input.readInt()
    val fingerprint = input.readBytes(fingerprintLength)
    val fingerprintMatches = kryo.manifest.fingerprint().contentEquals(fingerprint)

    val manifest = SnapshotManifest.readFrom(input)
    if (!fingerprintMatches) {
      log.info("Tried loading mismatched workspace snapshot, changes ${manifest.describeChangesAgainst(kryo.manifest)}")
      return WorkspaceSnapshot.EMPTY
    }

    val partial = kryo.pool.use { kryo -> kryo.readObject(input, PersistentWorkspaceSnapshot::class.java) }

    // the blob is the sole authority on the current generation, metadata.version IS the generation
    val generationId = partial.metadata.version
    if (!storage.hasGeneration(generationId)) {
      log.info("Workspace snapshot blob refer generation $generationId which is not in the db")
      return WorkspaceSnapshot.EMPTY
    }
    val generation = storage.openGeneration(generationId)

    return WorkspaceSnapshot(
      targets = PersistentWorkspaceTargetMap(partialSnapshot = partial, generation = generation),
      workspaceName = partial.workspaceName,
      configurations = partial.configurations,
      targetGraph = partial.targetGraph,
      fileToTarget = PersistentFileToTargetMap(partialSnapshot = partial, generation = generation),
      executableTargets = PersistentExecutableTargetsIndex(partialSnapshot = partial, generation = generation),
      syncConfigs = partial.syncConfigs,
      repoMapping = partial.repoMapping,
      metadata = partial.metadata,
    )
  }

  private suspend fun store(snapshot: WorkspaceSnapshot) {
    storeLock.withLock {
      if (storage.isClosed) {
        return
      }
      if (snapshot === lastStoredSnapshot || snapshot === lastStoredSource) {
        return
      }
      try {
        val time = measureTime { storeImpl(snapshot) }
        log.info("`WorkspaceSnapshot` store time: ${time.toString(DurationUnit.MILLISECONDS, 2)}")
      }
      catch (e: CancellationException) {
        throw e
      }
      catch (e: Throwable) {
        log.error("`WorkspaceSnapshot` store failed", e)
      }
    }
  }

  // TODO: maybe some background progress for this thing?
  private suspend fun storeImpl(snapshot: WorkspaceSnapshot) {
    val backing = snapshot.targets as? PersistentWorkspaceTargetMap
    if (backing != null && !backing.generation.isLive()) {
      log.warn("skipping store: snapshot reads through retired generation ${backing.generation.generation}")
      return
    }

    val previousGeneration = lastStoredSnapshot.metadata.version
    val newGeneration = previousGeneration + 1

    val allTargets = snapshot.targets.allTargets().toList()

    // always persist previous generation
    val snapshotBackingGeneration = (snapshot.targets as? PersistentWorkspaceTargetMap)?.generation?.generation
    val keep = buildSet {
      add(previousGeneration)
      snapshotBackingGeneration?.let { add(it) }
    }
    storage.sweepGenerationsExcept(keep)
    val generation = storage.createGeneration(newGeneration)

    val keyId2Target = Int2ObjectBiMap<WorkspaceTargetKey>()
    val labelId2Label = Int2ObjectBiMap<Label>()
    val hash2KeyIds = Long2ObjectOpenHashMap<IntArrayList>()
    val targetsToSave = ArrayList<WorkspaceTargetToSave>(allTargets.size)
    var nextKeyId = 1
    var nextLabelId = 1
    for (raw in allTargets) {
      // setup global key/label IDs
      val key = raw.key
      val keyId = nextKeyId++
      keyId2Target[keyId] = key

      // keep labelId2Label bijective to correctly handle duplicated labels
      if (labelId2Label.getReverseOrDefault(key.label, -1) < 0) {
        labelId2Label[nextLabelId++] = key.label
      }

      // prepare saved target
      targetsToSave += WorkspaceTargetToSave(keyId = keyId, target = raw)

      // prepare file map
      raw.allSources.forEach { file -> hash2KeyIds.computeIfAbsent(hashFilePath(file)) { IntArrayList() }.add(keyId) }
    }

    val pendingDelta = computePendingFileMappingDelta(
      snapshot = snapshot, hash2KeyIds = hash2KeyIds, keyId2Target = keyId2Target,
    )

    // targets are serialized in parallel on Dispatchers.Default, then appended
    // by this coroutine in keyId order
    generation.saveTargets(targets = targetsToSave)
    generation.saveFileToTargetMap(map = hash2KeyIds)
    saveExecutableTargetIndex(
      snapshot = snapshot,
      allTargets = allTargets,
      labelId2Label = labelId2Label,
      generation = generation,
      lastLabelId = nextLabelId,
    )

    val partial = PersistentWorkspaceSnapshot(
      // copies
      workspaceName = snapshot.workspaceName,
      configurations = snapshot.configurations,
      targetGraph = snapshot.targetGraph,
      syncConfigs = snapshot.syncConfigs,
      repoMapping = snapshot.repoMapping,
      metadata = snapshot.metadata.copy(version = newGeneration),

      // extras
      keyId2Target = keyId2Target,
      labelId2Label = labelId2Label,
    )

    val blobTmp = try {
      writeBlobTmp(partial)
    }
    catch (e: Throwable) {
      // rollback when blob part save failed
      storage.rollback()
      log.error("Error saving workspace snapshot", e)
      return
    }

    try {
      // try committing database first, only then try to persist blob
      storage.commit()
      try {
        blobTmp.moveTo(blobFile, StandardCopyOption.ATOMIC_MOVE)
      }
      catch (_: AtomicMoveNotSupportedException) {
        blobTmp.moveTo(target = blobFile, overwrite = true)
      }
    }
    catch (e: Throwable) {
      // commit failure, abort
      blobTmp.deleteIfExists()
      throw e
    }

    log.info("`WorkspaceSnapshot` stored generation $newGeneration (${allTargets.size} targets)")

    // swap snapshot lazy-loadable fields
    val twin = WorkspaceSnapshot(
      targets = PersistentWorkspaceTargetMap(partialSnapshot = partial, generation = generation),
      workspaceName = partial.workspaceName,
      configurations = partial.configurations,
      targetGraph = partial.targetGraph,
      fileToTarget = PersistentFileToTargetMap(
        partialSnapshot = partial,
        generation = generation,
        delta = pendingDelta ?: ConcurrentHashMap(),
      ),
      executableTargets = PersistentExecutableTargetsIndex(partialSnapshot = partial, generation = generation),
      syncConfigs = partial.syncConfigs,
      repoMapping = partial.repoMapping,
      metadata = partial.metadata,
    )
    // set BEFORE publishing, the debounce collector must skip the twin emission
    lastStoredSnapshot = twin
    lastStoredSource = snapshot
    commits.compareAndSet(snapshot, twin)

    // persist only published and new generations, do this so we don't remove generation under opened cursors
    // however in most cases newGeneration == published, they can be different only in case of concurrent writes which are rare
    val publishedBacking = (commits.value.targets as? PersistentWorkspaceTargetMap)?.generation?.generation
    storage.sweepGenerationsExcept(
      buildSet {
        add(newGeneration)
        publishedBacking?.let { add(it) }
      },
    )
    storage.commit()
    storage.compactFile(COMPACT_TIME_BUDGET_MILLIS)
  }

  private fun saveExecutableTargetIndex(
    snapshot: WorkspaceSnapshot,
    allTargets: List<BuildTarget>,
    labelId2Label: Int2ObjectBiMap<Label>,
    generation: SnapshotGeneration,
    lastLabelId: Int,
  ) {
    // executable index is derived data: recompute from the targets being stored, persist as keyIds
    val importDepth = snapshot.syncConfigs.filterIsInstance<CommonWorkspaceSyncConfig>().firstOrNull()?.importDepth ?: -1
    val hash2ExecutableKeys = ExecutableTargetsIndexBuilder.buildExecutableTargetsIndex(
      targetGraph = snapshot.targetGraph,
      importDepth = importDepth,
      targets = allTargets,
    )
    val map = Int2ObjectOpenHashMap<IntArrayList>()
    var nextLabelId = lastLabelId
    for ((label, executables) in hash2ExecutableKeys) {
      // label might contain macro/generator name which isn't present in `labelId2Label` so add it right now
      var labelId = labelId2Label.getReverseOrDefault(label, -1)
      if (labelId < 0) {
        labelId = nextLabelId++
        labelId2Label[labelId] = label
      }
      val executableList = map.computeIfAbsent(labelId) { IntArrayList() }
      for (executable in executables) {
        val executableId = labelId2Label.getReverseOrDefault(executable, -1)
        if (executableId < 0) {
          continue
        }
        executableList.add(executableId)
      }
    }
    generation.saveExecutableTargets(map)
  }

  // TODO: for now storage incremental updates are implemented in monolithic/naive way
  //  we should aim for providing diffing between previous and current snapshot and based on that flush only necessary object
  private fun computePendingFileMappingDelta(
    snapshot: WorkspaceSnapshot,
    hash2KeyIds: Long2ObjectOpenHashMap<IntArrayList>,
    keyId2Target: Int2ObjectBiMap<WorkspaceTargetKey>,
  ): ConcurrentHashMap<Long, List<WorkspaceTargetKey>>? {
    val pendingDelta = when (val fileToTarget = snapshot.fileToTarget) {
      is InMemoryFileToTargetMap -> fileToTarget.delta
      is PersistentFileToTargetMap -> fileToTarget.delta
      else -> null
    }
    if (pendingDelta != null) {
      for ((hash, keys) in pendingDelta) {
        if (keys.isEmpty()) {
          hash2KeyIds.remove(hash)
          continue
        }
        val keyIds = IntArrayList(keys.size)
        for (key in keys) {
          val keyId = keyId2Target.getReverseOrDefault(key, -1)
          if (keyId > 0) {
            keyIds.add(keyId)
          }
        }
        if (keyIds.isEmpty) {
          hash2KeyIds.remove(hash)
        }
        else {
          hash2KeyIds.put(hash, keyIds)
        }
      }
    }
    return pendingDelta
  }

  private fun writeBlobTmp(partial: PersistentWorkspaceSnapshot): Path {
    // TODO: can be optimized to zero-copy file channel
    val output = ByteBufferOutput(DEFAULT_BUFFER_SIZE, -1)

    // format version
    output.writeInt(SNAPSHOT_FORMAT_VERSION)

    // fingerprint
    val fingerprint = kryo.manifest.fingerprint()
    output.writeInt(fingerprint.size)
    output.writeBytes(fingerprint)

    // manifest itself
    kryo.manifest.writeTo(output)

    // last snapshot payload
    kryo.pool.use { kryo -> kryo.writeObject(output, partial) }

    val blobTmp = blobFile.resolveSibling("${blobFile.fileName}.tmp")
    blobTmp.writeBytes(output.toBytes())
    return blobTmp
  }

  override fun dispose() {
    if (storageLazy.isInitialized()) {
      storage.close()
    }
  }
}
