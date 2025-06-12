@file:Suppress("ReplacePutWithAssignment", "ReplaceGetOrSet")

package org.jetbrains.bazel.target

import com.dynatrace.hash4j.hashing.HashValue128
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.io.mvstore.createOrResetMvStore
import com.intellij.util.io.mvstore.openOrResetMap
import org.h2.mvstore.DataUtils.readVarInt
import org.h2.mvstore.MVMap
import org.h2.mvstore.MVStore
import org.jetbrains.bazel.label.Apparent
import org.jetbrains.bazel.label.Canonical
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toCanonicalLabel
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sdkcompat.HashAdapter
import org.jetbrains.bazel.sdkcompat.createHashStream128
import org.jetbrains.bazel.sdkcompat.hashBytesTo128Bits
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.PartialBuildTarget
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
  private val moduleIdToTarget: MVMap<HashValue128, Label> = openIdToResolvedLabelMap(store, "moduleIdToTarget", logSupplier)

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
    if (path.startsWith(filePathSuffix)) {
      return hashBytesTo128Bits(path.substring(filePathSuffix.length).toByteArray())
    } else {
      return hashBytesTo128Bits(path.toByteArray())
    }
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

  fun allBuildTargetAsLabelToTargetMap(): Map<Label, BuildTarget> {
    val result = HashMap<Label, BuildTarget>(labelToTargetInfo.size)
    val cursor = labelToTargetInfo.cursor(null)
    while (cursor.hasNext()) {
      cursor.next()
      val target = cursor.value
      result.put(target.id, target)
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
    label.toCanonicalLabel(project)?.let {
      labelToTargetInfo.get(computeLabelHash(it, createHashStream128()))
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
    val hashStream = createHashStream128()
    for ((label, info) in labelToTargetInfo) {
      // must be canonical label
      this.labelToTargetInfo.put(
        computeLabelHash(label, hashStream),
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
    val hashStream = createHashStream128()
    for (target in targets) {
      // must be canonical label
      val label = target.id as CanonicalLabel
      this.labelToTargetInfo.put(
        computeLabelHash(label, hashStream),
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
    store.close(300)
  }
}

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
        writeLabel(buffer, item)
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
): MVMap<HashValue128, Label> {
  val mapBuilder = MVMap.Builder<HashValue128, Label>()
  mapBuilder.setKeyType(HashValue128KeyDataType)
  mapBuilder.setValueType(createAnyValueDataType<Label>(writer = ::writeLabel, reader = ::readLabel))
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
          writeLabel(buffer, label)
        }
      },
      reader = { buffer ->
        Array(readVarInt(buffer)) { readLabel(buffer) }.asList()
      },
    ),
  )
  return openOrResetMap(store = store, name = name, mapBuilder = mapBuilder, logSupplier = logSupplier)
}

private fun stringToHashId(s: String): HashValue128 = hashBytesTo128Bits(s.encodeToByteArray())

private fun computeLabelHash(label: Label, hash: HashAdapter): HashValue128 {
  hashLabelRepo(label, hash)
  hashLabelPackage(label, hash)
  hashLabelTarget(label, hash)
  return hash.getAndReset()
}

private fun hashLabelRepo(label: Label, hash: HashAdapter) {
  when (val repo = label.repo) {
    is Canonical -> {
      hash.putByte(1)
      hash.putByteArray(repo.repoName.toByteArray())
    }

    is Apparent -> {
      hash.putByte(2)
      hash.putByteArray(repo.repoName.toByteArray())
    }
  }
}

private fun hashLabelPackage(label: Label, hash: HashAdapter) {
  val packagePath = label.packagePath
  for (string in packagePath.pathSegments) {
    hash.putByteArray(string.toByteArray())
  }
  hash.putInt(packagePath.pathSegments.size)
}

private fun hashLabelTarget(label: Label, hash: HashAdapter) {
  hash.putByteArray(label.targetName.toByteArray())
}
