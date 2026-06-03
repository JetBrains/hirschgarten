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
import org.jetbrains.annotations.ApiStatus
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
import org.jetbrains.bazel.magicmetamodel.formatAsModuleName
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.PartialBuildTarget
import java.nio.ByteBuffer
import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString

private val logger = logger<TargetsCacheStorage>()
private val logSupplier = { logger }

// The implementation heavily relies on hashing without conflicts
@ApiStatus.Internal
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
          kind = info.kind,
          baseDirectory = info.baseDirectory,
          data = info.data,
          isManual = info.isManual,
          isWorkspace = info.isWorkspace
        ),
      )
    }
  }

  fun reset(
    fileToTarget: Map<Path, List<Label>>,
    executableTargets: Map<ResolvedLabel, List<Label>>,
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

    this.labelToTargetInfo.clear()
    for (target in targets) {
      // must be canonical label
      val label = target.id as ResolvedLabel
      val labelHash = hashStream.computeLabelHash(label)
      this.labelToTargetInfo.put(
        labelHash,
        PartialBuildTarget(
          id = target.id,
          kind = target.kind,
          baseDirectory = target.baseDirectory,
          data = target.data,
          isManual = target.isManual,
          isWorkspace = target.isWorkspace
        ),
      )
    }
  }

  fun getTotalFileCount(): Int = fileToTargets.size

  fun close() {
    // close without compaction - close as fast as possible
    store.close()
  }
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
