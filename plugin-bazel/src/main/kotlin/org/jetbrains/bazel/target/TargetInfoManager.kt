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
import org.h2.mvstore.WriteBuffer
import org.jetbrains.bazel.commons.gson.bazelGson
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
import org.jetbrains.bazel.languages.starlark.repomapping.toCanonicalLabel
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bazel.sdkcompat.HashAdapter
import org.jetbrains.bazel.sdkcompat.createHashStream128
import org.jetbrains.bazel.sdkcompat.hashBytesTo128Bits
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
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

  private val labelToTargetInfo: MVMap<HashValue128, BuildTarget> = createIdToBuildTargetMap(store, logSupplier)

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
      this.labelToTargetInfo.put(computeLabelHash(label as ResolvedLabel, hashStream), info)
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
      val label = target.id as ResolvedLabel
      this.labelToTargetInfo.put(computeLabelHash(label, hashStream), target)

      moduleIdToTarget.put(stringToHashId(label.formatAsModuleName(project)), label)
    }
  }

  fun getTotalFileCount(): Int = fileToTarget.size

  fun close() {
    store.close(300)
  }
}

private fun createIdToBuildTargetMap(store: MVStore, logSupplier: () -> Logger): MVMap<HashValue128, BuildTarget> {
  val mapBuilder = MVMap.Builder<HashValue128, BuildTarget>()
  mapBuilder.setKeyType(HashValue128KeyDataType)
  val buildTargetInfoValueType =
    createAnyValueDataType<BuildTarget>(
      writer = { buffer, item ->
        // don't store dependencies, sources, resources as they are saved in the workspace model already
        val data =
          bazelGson
            .toJson(item.copy(dependencies = emptyList(), sources = emptyList(), resources = emptyList()))
            .encodeToByteArray()
        buffer.putVarInt(data.size)
        buffer.put(data)
      },
      reader = { buffer ->
        bazelGson.fromJson(buffer.readString(), BuildTarget::class.java)
      },
    )
  mapBuilder.setValueType(
    buildTargetInfoValueType,
  )
  return openOrResetMap(store = store, name = "labelToTargetInfo", mapBuilder = mapBuilder, logSupplier = logSupplier)
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
  mapBuilder.setValueType(
    createAnyValueDataType<ResolvedLabel>(
      writer = { buffer, label ->
        val repo = label.repo
        when (repo) {
          Main -> buffer.put(0)
          is Canonical -> {
            buffer.put(1)
            buffer.writeString(repo.repoName)
          }

          is Apparent -> {
            buffer.put(2)
            buffer.writeString(repo.repoName)
          }
        }

        val packagePath = label.packagePath
        when (packagePath) {
          is AllPackagesBeneath -> buffer.put(0)
          is Package -> buffer.put(1)
        }
        buffer.putVarInt(packagePath.pathSegments.size)
        for (value in packagePath.pathSegments) {
          buffer.writeString(value)
        }

        val target = label.target
        when (target) {
          AmbiguousEmptyTarget -> buffer.put(0)
          AllRuleTargets -> buffer.put(1)
          AllRuleTargetsAndFiles -> buffer.put(2)
          is SingleTarget -> {
            buffer.put(3)
            buffer.writeString(target.targetName)
          }
        }
      },
      reader = { buffer ->
        val repoType = buffer.get()
        val repo =
          when (repoType.toInt()) {
            0 -> Main
            1 -> Canonical.createCanonicalOrMain(buffer.readString())
            2 -> Apparent(buffer.readString())
            else -> throw IllegalStateException("Unexpected repo type $repoType")
          }

        val packageType = buffer.get()
        val packagePath =
          when (packageType.toInt()) {
            0 -> AllPackagesBeneath(Array(readVarInt(buffer)) { buffer.readString() }.asList())
            1 -> Package(Array(readVarInt(buffer)) { buffer.readString() }.asList())
            else -> throw IllegalStateException("Unexpected package type $packageType")
          }

        val targetType = buffer.get()
        val target =
          when (targetType.toInt()) {
            0 -> AmbiguousEmptyTarget
            1 -> AllRuleTargets
            2 -> AllRuleTargetsAndFiles
            3 -> SingleTarget(buffer.readString())
            else -> throw IllegalStateException("Unexpected target type $targetType")
          }

        ResolvedLabel(repo = repo, packagePath = packagePath, target = target)
      },
    ),
  )
  return openOrResetMap(store = store, name = name, mapBuilder = mapBuilder, logSupplier = logSupplier)
}

private fun ByteBuffer.readString(): String {
  val size = readVarInt(this)
  val bytes = ByteArray(size)
  get(bytes)
  return String(bytes)
}

private fun WriteBuffer.writeString(value: String) {
  val valueBytes = value.encodeToByteArray()
  putVarInt(valueBytes.size)
  put(valueBytes)
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

private fun stringToHashId(s: String): HashValue128 = hashBytesTo128Bits(s.encodeToByteArray())

private fun computeLabelHash(label: ResolvedLabel, hash: HashAdapter): HashValue128 {
  hashLabelRepo(label, hash)
  hashLabelPackage(label, hash)
  hashLabelTarget(label, hash)
  return hash.getAndReset()
}

private fun hashLabelRepo(label: ResolvedLabel, hash: HashAdapter) {
  val repo = label.repo
  when (repo) {
    Main -> hash.putByte(0)
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

private fun hashLabelPackage(label: ResolvedLabel, hash: HashAdapter) {
  val packagePath = label.packagePath
  when (packagePath) {
    is AllPackagesBeneath -> hash.putByte(0)
    is Package -> hash.putByte(1)
  }
  for (string in packagePath.pathSegments) {
    hash.putByteArray(string.toByteArray())
  }
  hash.putInt(packagePath.pathSegments.size)
}

private fun hashLabelTarget(label: ResolvedLabel, hash: HashAdapter) {
  val target = label.target
  when (target) {
    AmbiguousEmptyTarget -> hash.putByte(0)
    AllRuleTargets -> hash.putByte(1)
    AllRuleTargetsAndFiles -> hash.putByte(2)
    is SingleTarget -> {
      hash.putByte(3)
      hash.putByteArray(target.targetName.toByteArray())
    }
  }
}
