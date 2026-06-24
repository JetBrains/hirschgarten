package org.jetbrains.bazel.workspace.importer

import com.intellij.openapi.diagnostic.logger
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceAspectIds
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTarget
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.SourceFileCollection
import java.nio.file.Path

// RC: can be shared among other importers if needed
internal object JvmWorkspaceTargetMerger {
  private val log = logger<JvmWorkspaceTargetMerger>()

  fun mergeByTargetKey(targets: Collection<WorkspaceTarget>): List<WorkspaceTarget> =
    targets.groupBy { it.targetKey.copy(aspectIds = WorkspaceAspectIds.EMPTY) }
      .map { (key, group) ->
        if (group.size == 1) {
          WorkspaceTarget(targetKey = key, rawBuildTarget = group.single().rawBuildTarget.copy(key = key))
        }
        else {
          // keep it deterministic
          group.sortedBy { it.targetKey.toString() }
            .reduce { l, r -> merge(key, l, r) }
        }
      }

  private fun merge(key: WorkspaceTargetKey, left: WorkspaceTarget, right: WorkspaceTarget): WorkspaceTarget {
    if (!left.isCompatibleWith(right)) {
      log.warn("Trying to merge incompatible ${WorkspaceTarget::class}, ${key}, workspace model could be incorrect.")
    }

    val rawLeft = left.rawBuildTarget
    val rawRight = right.rawBuildTarget
    return WorkspaceTarget(
      targetKey = key,
      rawBuildTarget = rawLeft.copy(
        key = key,

        // dependencies might be composed of multiple providers, so merge manually
        dependencies = (rawLeft.dependencies + rawRight.dependencies)
          .distinctBy { it.copy(targetKey = it.targetKey.copy(aspectIds = WorkspaceAspectIds.EMPTY)) },

        sources = mergeFileCollections(rawLeft.sources, rawRight.sources),
        generatedSources = mergeFileCollections(rawLeft.generatedSources, rawRight.generatedSources),
        resources = mergeFileCollections(rawLeft.resources, rawRight.resources),

        // the most important part of merging
        data = mergeBuildData(rawLeft.data.asSequence() + rawRight.data.asSequence()),
      ),
    )
  }

  private fun mergeFileCollections(left: SourceFileCollection, right: SourceFileCollection): SourceFileCollection {
    if (left == right) {
      return left
    }
    // merging, building trie inside another trie :p
    return object : SourceFileCollection {
      override fun isEmpty(): Boolean = left.isEmpty() && right.isEmpty()
      override fun getFiles(): Sequence<Path> = (left.getFiles() + right.getFiles()).distinct()
    }
  }

  private fun WorkspaceTarget.isCompatibleWith(other: WorkspaceTarget): Boolean {
    val left = this.rawBuildTarget
    val right = other.rawBuildTarget
    return left.kind == right.kind
           && left.baseDirectory == right.baseDirectory
           && left.generatorName == right.generatorName
           && left.isManual == right.isManual
           && left.isWorkspace == right.isWorkspace
           && left.isTestOnly == right.isTestOnly
  }

  fun interface MergeFunction<T : BuildTargetData> {
    operator fun invoke(left: T, right: T): T
  }

  private val mergingFunctions = mapOf(
    JvmBuildTarget::class to MergeFunction<JvmBuildTarget> { left, right ->
      return@MergeFunction left.copy(
        binaryOutputs = mergeFileCollections(left.binaryOutputs, right.binaryOutputs),
        libraries = (left.libraries + right.libraries).distinct(),
        jvmDependencies = (left.jvmDependencies + right.jvmDependencies)
          .distinctBy { it::class to it.dependency.copy(targetKey = it.dependency.targetKey.copy(aspectIds = WorkspaceAspectIds.EMPTY)) },
      )
    },

    KotlinBuildTarget::class to MergeFunction<KotlinBuildTarget> { left, right ->
      return@MergeFunction left.copy(
        associates = (left.associates + right.associates).distinct(),
      )
    },
  )

  private fun mergeBuildData(input: Sequence<BuildTargetData>): List<BuildTargetData> {
    // we have to merge them, `BuildTargetData` doesn't correspond to specific provider
    // BuildTargetData` can overlap relative to source provider
    return input.groupBy { it::class }
      .map { (type, data) ->
        when {
          data.size == 1 -> data.single()
          else -> {
            @Suppress("UNCHECKED_CAST")
            val fn = mergingFunctions[type] as? MergeFunction<BuildTargetData>?
            if (fn == null) {
              // only warn when candidates are indeed different
              val allEqual = data.all { it == data.first() }
              if (!allEqual) {
                log.warn("Trying to merge unsupported ${BuildTargetData::class}, ${type}, workspace model could be incorrect.")
              }
              data.first()
            }
            else {
              data.reduce { l, r -> fn(l, r) }
            }
          }
        }
      }
  }
}
