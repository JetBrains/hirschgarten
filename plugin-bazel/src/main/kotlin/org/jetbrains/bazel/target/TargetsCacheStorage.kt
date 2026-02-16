@file:Suppress("ReplacePutWithAssignment", "ReplaceGetOrSet")

package org.jetbrains.bazel.target

import com.dynatrace.hash4j.hashing.HashSink
import com.dynatrace.hash4j.hashing.HashStream64
import com.dynatrace.hash4j.hashing.Hashing
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.io.mvstore.createOrResetMvStore
import com.intellij.util.io.mvstore.openOrResetMap
import org.h2.mvstore.DataUtils.readVarInt
import org.h2.mvstore.MVMap
import org.h2.mvstore.MVStore
import org.h2.mvstore.WriteBuffer
import org.h2.mvstore.type.LongDataType
import org.jetbrains.bazel.label.AllPackagesBeneath
import org.jetbrains.bazel.label.AllRuleTargets
import org.jetbrains.bazel.label.AllRuleTargetsAndFiles
import org.jetbrains.bazel.label.AmbiguousEmptyTarget
import org.jetbrains.bazel.label.Apparent
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.Package
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.label.SingleTarget
import org.jetbrains.bazel.languages.starlark.repomapping.toCanonicalLabelOrThis
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.PartialBuildTarget
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString

private val logger = logger<TargetsCacheStorage>()
private val logSupplier = { logger }

// The implementation heavily relies on hashing without conflicts
class TargetsCacheStorage(
  private val store: MVStore,
  private val project: Project
) {
  companion object {
    fun openStore(storeFile: Path, project: Project): TargetsCacheStorage {
      val store = createOrResetMvStore(storeFile, readOnly = false, logSupplier = logSupplier)
      return TargetsCacheStorage(store, project)
    }
  }
  private val filePathSuffix: String = project.basePath!! + "/"

  private val targetToExecutableTargets: MVMap<Long, List<Long>> = openIdToLabelHashListMap("targetToExecutableTargets", store)
  private val fileToTargets: MVMap<Long, List<Long>> = openIdToLabelHashListMap("fileToTargets", store)
  private val libraryIdToTarget: MVMap<Long, Label> = openIdToLabelMap(store, "libraryIdToTarget")
  private val moduleIdToTarget: MVMap<Long, Long> = openIdToIdMap(store, "moduleIdToTarget")
  private val labelToTargetInfo: MVMap<Long, PartialBuildTarget> =
    openOrResetMap(
      store = store,
      name = "labelToTargetInfo",
      mapBuilder = createIdToBuildMapType(filePathSuffix, Path.of(filePathSuffix.trimEnd('/'))),
      logSupplier = logSupplier,
    )

  fun save() {
    if (store.hasUnsavedChanges()) {
      store.tryCommit()
    }
  }

  private fun fileToKey(file: Path): Long {
    val path = file.invariantSeparatorsPathString
    val input = if (path.startsWith(filePathSuffix)) {
      path.substring(filePathSuffix.length)
    } else {
      path
    }

    return Hashing.xxh3_64()
      .hashBytesToLong(input.toByteArray())
  }

  fun getAllTargetsAndLibrariesLabelsCache(): List<String> {
    val result = ArrayList<String>(labelToTargetInfo.size + libraryIdToTarget.size)
    run {
      val cursor = labelToTargetInfo.cursor(null)
      while (cursor.hasNext()) {
        cursor.next()
        result.add(cursor.value.id.toShortString(project))
      }
    }
    run {
      val cursor = libraryIdToTarget.cursor(null)
      while (cursor.hasNext()) {
        cursor.next()
        result.add(cursor.value.toShortString(project))
      }
    }
    return result
  }

  fun getAllBuildTargets(): Sequence<BuildTarget> =
    sequence {
      val cursor = labelToTargetInfo.cursor(null)
      while (cursor.hasNext()) {
        cursor.next()
        yield(cursor.value)
      }
    }

  fun allBuildTargetAsLabelToTargetMap(predicate: (BuildTarget) -> Boolean): List<Label> {
    val result = ArrayList<Label>(labelToTargetInfo.size)
    val cursor = labelToTargetInfo.cursor(null)
    while (cursor.hasNext()) {
      cursor.next()
      val target = cursor.value
      if (predicate(target)) {
        result.add(target.id)
      }
    }
    return result
  }

  fun getAllTargets(): Sequence<Label> =
    sequence {
      val cursor = labelToTargetInfo.cursor(null)
      while (cursor.hasNext()) {
        cursor.next()
        yield(cursor.value.id)
      }
    }

  fun computeFullLabelToTargetInfoMap(syncedTargetIdToTargetInfo: Map<Label, BuildTarget>): Map<Label, BuildTarget> {
    val result = HashMap<Label, BuildTarget>(labelToTargetInfo.size + syncedTargetIdToTargetInfo.size)
    // first, an existing map, and then a new addition
    val cursor = labelToTargetInfo.cursor(null)
    while (cursor.hasNext()) {
      cursor.next()
      val target = cursor.value
      result.put(target.id, target)
    }

    result.putAll(syncedTargetIdToTargetInfo)
    return result
  }

  fun getTotalTargetCount() = labelToTargetInfo.size

  fun getBuildTargetForLabel(label: Label): BuildTarget? =
    label.toCanonicalLabelOrThis(project)?.let { label ->
      labelToTargetInfo.get(computeLabelHash(label))
    }

  fun getTargetsForPath(file: Path): List<Label>? = fileToTargets.get(fileToKey(file))?.mapNotNull { labelToTargetInfo.get(it)?.id }

  fun getExecutableTargetsForTarget(target: Label): List<Label>? =
    target.toCanonicalLabelOrThis(project)?.let {
      targetToExecutableTargets.get(computeLabelHash(it))
    }?.mapNotNull { labelToTargetInfo.get(it)?.id }

  fun addFileToTarget(file: Path, targets: List<Label>) {
    fileToTargets.put(fileToKey(file), targets.map { computeLabelHash(it as ResolvedLabel) })
  }

  fun removeFileToTarget(file: Path) {
    fileToTargets.remove(fileToKey(file))
  }

  fun getTargetForLibraryId(libraryId: String): Label? = 
    libraryIdToTarget.get(stringToHashId(libraryId))

  fun getTargetForModuleId(libraryId: String): ResolvedLabel? =
    moduleIdToTarget.get(stringToHashId(libraryId))
      ?.let { labelToTargetInfo.get(it)?.id as ResolvedLabel? }

  fun setTargets(targets: List<BuildTarget>) {
    this.labelToTargetInfo.clear()
    val hashStream = Hashing.xxh3_64()
      .hashStream()
    for (info in targets) {
      // must be canonical label
      this.labelToTargetInfo.put(
        hashStream.computeLabelHash(info.id as ResolvedLabel),
        PartialBuildTarget(
          id = info.id,
          tags = info.tags,
          kind = info.kind,
          baseDirectory = info.baseDirectory,
          data = info.data,
          noBuild = info.noBuild,
        ),
      )
    }
  }

  fun reset(
    fileToTarget: Map<Path, List<Label>>,
    executableTargets: Map<ResolvedLabel, List<Label>>,
    libraryItems: List<LibraryItem>,
    targets: List<BuildTarget>,
  ) {
    val hashStream = Hashing.xxh3_64()
      .hashStream()

    // todo we can optimize by constructing such a map in our sync instead of transformation
    this.fileToTargets.clear()
    for ((file, targets) in fileToTarget) {
      this.fileToTargets.put(fileToKey(file), targets.map { hashStream.computeLabelHash(it as ResolvedLabel) })
    }

    this.targetToExecutableTargets.clear()
    for ((label, targets) in executableTargets) {
      this.targetToExecutableTargets.put(hashStream.computeLabelHash(label), targets.map { hashStream.computeLabelHash(it as ResolvedLabel) })
    }

    this.libraryIdToTarget.clear()
    for (library in libraryItems) {
      this.libraryIdToTarget.put(stringToHashId(library.id.formatAsModuleName(project)), library.id)
    }

    this.moduleIdToTarget.clear()
    this.labelToTargetInfo.clear()
    for (target in targets) {
      // must be canonical label
      val label = target.id as ResolvedLabel
      val labelHash = hashStream.computeLabelHash(label)
      this.labelToTargetInfo.put(
        labelHash,
        PartialBuildTarget(
          id = target.id,
          tags = target.tags,
          kind = target.kind,
          baseDirectory = target.baseDirectory,
          data = target.data,
          noBuild = target.noBuild,
        ),
      )

      this.moduleIdToTarget.put(stringToHashId(label.formatAsModuleName(project)), labelHash)
    }
  }

  fun getTotalFileCount(): Int = fileToTargets.size

  fun close() {
    // close without compaction - close as fast as possible
    store.close()
  }
}

private fun writeLabel(item: Label, buffer: WriteBuffer) {
  buffer.writeString(item.toString())
}

private fun readLabel(buffer: ByteBuffer): Label = Label.parse(buffer.readString())

private fun openIdToLabelMap(
  store: MVStore,
  @Suppress("SameParameterValue") name: String
): MVMap<Long, Label> {
  val mapBuilder = MVMap.Builder<Long, Label>()
  mapBuilder.setKeyType(LongDataType.INSTANCE)
  mapBuilder.setValueType(
    createAnyValueDataType(
      writer = { buffer, item ->
        writeLabel(item, buffer)
      },
      reader = {
        readLabel(it)
      },
    ),
  )
  return openOrResetMap(store = store, name = name, mapBuilder = mapBuilder, logSupplier = logSupplier)
}

private fun openIdToIdMap(
  store: MVStore,
  @Suppress("SameParameterValue") name: String
): MVMap<Long, Long> {
  val mapBuilder = MVMap.Builder<Long, Long>()
  mapBuilder.setKeyType(LongDataType.INSTANCE)
  mapBuilder.setValueType(LongDataType.INSTANCE)
  return openOrResetMap(store = store, name = name, mapBuilder = mapBuilder, logSupplier = logSupplier)
}

private fun openIdToLabelHashListMap(
  name: String,
  store: MVStore
): MVMap<Long, List<Long>> {
  val mapBuilder = MVMap.Builder<Long, List<Long>>()
  mapBuilder.setKeyType(LongDataType.INSTANCE)
  mapBuilder.setValueType(
    createAnyValueDataType<List<Long>>(
      writer = { buffer, list ->
        buffer.putVarInt(list.size)
        for (label in list) {
          buffer.putLong(label)
        }
      },
      reader = { buffer ->
        val size = readVarInt(buffer)
        ArrayList<Long>(size).apply {
          repeat(size) {
            add(buffer.getLong())
          }
        }
      },
    ),
  )
  return openOrResetMap(store = store, name = name, mapBuilder = mapBuilder, logSupplier = logSupplier)
}

private fun stringToHashId(s: String): Long =
  Hashing.xxh3_64()
    .hashBytesToLong(s.encodeToByteArray())

private fun computeLabelHash(label: ResolvedLabel): Long =
  Hashing.xxh3_64()
    .hashStream()
    .computeLabelHash(label)

private fun HashStream64.computeLabelHash(label: ResolvedLabel): Long {
  hashLabelRepo(label)
  hashLabelPackage(label)
  hashLabelTarget(label)

  val result = getAsLong()
  reset()
  return result
}

private fun HashSink.hashLabelRepo(label: ResolvedLabel) {
  when (val repo = label.repo) {
    Main -> putByte(0)
    is Canonical -> {
      putByte(1)
      putByteArray(repo.repoName.toByteArray())
    }

    is Apparent -> {
      putByte(2)
      putByteArray(repo.repoName.toByteArray())
    }
  }
}

private fun HashSink.hashLabelPackage(label: ResolvedLabel) {
  val packagePath = label.packagePath
  when (packagePath) {
    is AllPackagesBeneath -> putByte(0)
    is Package -> putByte(1)
  }
  for (string in packagePath.pathSegments) {
    putByteArray(string.toByteArray())
  }
  putInt(packagePath.pathSegments.size)
}

private fun HashSink.hashLabelTarget(label: ResolvedLabel) {
  when (val target = label.target) {
    AmbiguousEmptyTarget -> putByte(0)
    AllRuleTargets -> putByte(1)
    AllRuleTargetsAndFiles -> putByte(2)
    is SingleTarget -> {
      putByte(3)
      putByteArray(target.targetName.toByteArray())
    }
  }
}
