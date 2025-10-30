@file:Suppress("ReplacePutWithAssignment", "ReplaceGetOrSet")

package org.jetbrains.bazel.target

import com.dynatrace.hash4j.hashing.HashSink
import com.dynatrace.hash4j.hashing.HashStream128
import com.dynatrace.hash4j.hashing.HashValue128
import com.dynatrace.hash4j.hashing.Hashing
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.io.mvstore.createOrResetMvStore
import com.intellij.util.io.mvstore.openOrResetMap
import org.h2.mvstore.DataUtils.readVarInt
import org.h2.mvstore.MVMap
import org.h2.mvstore.MVStore
import org.h2.mvstore.WriteBuffer
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

internal fun openStore(storeFile: Path, filePathSuffix: String): TargetInfoManager {
  val logSupplier = { logger<TargetInfoManager>() }
  val store = createOrResetMvStore(storeFile, readOnly = false, logSupplier = logSupplier)
  return TargetInfoManager(store, filePathSuffix, logSupplier)
}

internal class TargetInfoManager(
  private val store: MVStore,
  private val filePathSuffix: String,
  logSupplier: () -> Logger,
) {
  private val fileToExecutableTargets: MVMap<HashValue128, List<Label>> =
    openIdToLabelListMap("fileToExecutableTargets", store, logSupplier)
  private val fileToTarget: MVMap<HashValue128, List<Label>> = openIdToLabelListMap("fileToTarget", store, logSupplier)

  private val libraryIdToTarget: MVMap<HashValue128, Label> = openIdToLabelMap(store, "libraryIdToTarget", logSupplier)
  private val moduleIdToTarget: MVMap<HashValue128, ResolvedLabel> = openIdToResolvedLabelMap(store, "moduleIdToTarget", logSupplier)

  private val labelToTargetInfo: MVMap<HashValue128, PartialBuildTarget> =
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

  private fun fileToKey(file: Path): HashValue128 {
    val path = file.invariantSeparatorsPathString
    val input = if (path.startsWith(filePathSuffix)) {
      path.substring(filePathSuffix.length)
    } else {
      path
    }

    return Hashing.xxh3_128()
      .hashBytesTo128Bits(input.toByteArray())
  }

  fun getAllTargetsAndLibrariesLabelsCache(project: Project): List<String> {
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

  fun getBuildTargetForLabel(label: Label, project: Project): BuildTarget? =
    label.toCanonicalLabelOrThis(project)?.let { label ->
      val key = Hashing.xxh3_128()
        .hashStream()
        .computeLabelHash(label)
      labelToTargetInfo.get(key)
    }

  fun getTargetsForPath(file: Path) = fileToTarget.get(fileToKey(file))

  fun getExecutableTargetsForPath(file: Path) = fileToExecutableTargets.get(fileToKey(file))

  fun addFileToTarget(file: Path, targets: List<Label>) {
    fileToTarget.put(fileToKey(file), targets)
  }

  fun removeFileToTarget(file: Path) {
    fileToTarget.remove(fileToKey(file))
  }

  fun getTargetForLibraryId(libraryId: String) = libraryIdToTarget.get(stringToHashId(libraryId))

  fun getTargetForModuleId(libraryId: String) = moduleIdToTarget.get(stringToHashId(libraryId))

  fun setTargets(labelToTargetInfo: Map<Label, BuildTarget>) {
    this.labelToTargetInfo.clear()
    val hashStream = Hashing.xxh3_128()
      .hashStream()
    for ((label, info) in labelToTargetInfo) {
      // must be canonical label
      this.labelToTargetInfo.put(
        hashStream.computeLabelHash(label as ResolvedLabel),
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
    fileToExecutableTargets: Map<Path, List<Label>>,
    libraryItems: List<LibraryItem>?,
    project: Project,
    targets: List<BuildTarget>,
  ) {
    // todo we can optimize by constructing such a map in our sync instead of transformation
    this.fileToTarget.clear()
    for (entry in fileToTarget) {
      addFileToTarget(entry.key, entry.value)
    }

    this.fileToExecutableTargets.clear()
    for ((file, targets) in fileToExecutableTargets) {
      this.fileToExecutableTargets.put(fileToKey(file), targets)
    }

    if (libraryItems.isNullOrEmpty()) {
      libraryIdToTarget.clear()
    } else {
      for (library in libraryItems) {
        libraryIdToTarget.put(stringToHashId(library.id.formatAsModuleName(project)), library.id)
      }
    }

    moduleIdToTarget.clear()
    this.labelToTargetInfo.clear()
    val hashStream = Hashing.xxh3_128()
      .hashStream()
    for (target in targets) {
      // must be canonical label
      val label = target.id as ResolvedLabel
      this.labelToTargetInfo.put(
        hashStream.computeLabelHash(label),
        PartialBuildTarget(
          id = target.id,
          tags = target.tags,
          kind = target.kind,
          baseDirectory = target.baseDirectory,
          data = target.data,
          noBuild = target.noBuild,
        ),
      )

      moduleIdToTarget.put(stringToHashId(label.formatAsModuleName(project)), label)
    }
  }

  fun getTotalFileCount(): Int = fileToTarget.size

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
  @Suppress("SameParameterValue") name: String,
  logSupplier: () -> Logger,
): MVMap<HashValue128, Label> {
  val mapBuilder = MVMap.Builder<HashValue128, Label>()
  mapBuilder.setKeyType(HashValue128KeyDataType)
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

private fun openIdToResolvedLabelMap(
  store: MVStore,
  @Suppress("SameParameterValue") name: String,
  logSupplier: () -> Logger,
): MVMap<HashValue128, ResolvedLabel> {
  val mapBuilder = MVMap.Builder<HashValue128, ResolvedLabel>()
  mapBuilder.setKeyType(HashValue128KeyDataType)
  mapBuilder.setValueType(createAnyValueDataType<ResolvedLabel>(writer = ::writeResolvedLabel, reader = ::readResolvedLabel))
  return openOrResetMap(store = store, name = name, mapBuilder = mapBuilder, logSupplier = logSupplier)
}

private fun openIdToLabelListMap(
  name: String,
  store: MVStore,
  logSupplier: () -> Logger,
): MVMap<HashValue128, List<Label>> {
  val mapBuilder = MVMap.Builder<HashValue128, List<Label>>()
  mapBuilder.setKeyType(HashValue128KeyDataType)
  mapBuilder.setValueType(
    createAnyValueDataType<List<Label>>(
      writer = { buffer, list ->
        buffer.putVarInt(list.size)
        for (label in list) {
          writeLabel(label, buffer)
        }
      },
      reader = { buffer ->
        Array(readVarInt(buffer)) { readLabel(buffer) }.asList()
      },
    ),
  )
  return openOrResetMap(store = store, name = name, mapBuilder = mapBuilder, logSupplier = logSupplier)
}

private fun stringToHashId(s: String): HashValue128 =
  Hashing.xxh3_128()
    .hashBytesTo128Bits(s.encodeToByteArray())

private fun HashStream128.computeLabelHash(label: ResolvedLabel): HashValue128 {
  hashLabelRepo(label)
  hashLabelPackage(label)
  hashLabelTarget(label)

  val result = get()
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
