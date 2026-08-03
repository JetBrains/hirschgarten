package org.jetbrains.bazel.sync.workspace.snapshot

import com.intellij.openapi.diagnostic.logger
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.SourceFileCollection
import java.nio.file.Path
import kotlin.reflect.KClass

typealias MergeFunctionMap = Map<KClass<out BuildTargetData>, MergeFunction<*>>

@ApiStatus.Internal
fun interface MergeFunction<T : BuildTargetData> {
  operator fun invoke(left: T, right: T): T
}

@ApiStatus.Internal
class WorkspaceTargetMerger(val mergeFunctions: MergeFunctionMap) {
  companion object {
    private val log = logger<WorkspaceTargetMerger>()
  }

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

        // merge language classes
        kind = rawLeft.kind.copy(languageClasses = rawLeft.kind.languageClasses + rawRight.kind.languageClasses),

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

  private fun WorkspaceTarget.isCompatibleWith(other: WorkspaceTarget): Boolean {
    val left = this.rawBuildTarget
    val right = other.rawBuildTarget
    return left.kind.kind == right.kind.kind
           && left.kind.ruleType == right.kind.ruleType
           && left.baseDirectory == right.baseDirectory
           && left.generatorName == right.generatorName
           && left.isManual == right.isManual
           && left.isWorkspace == right.isWorkspace
           && left.isTestOnly == right.isTestOnly
  }

  private fun mergeBuildData(input: Sequence<BuildTargetData>): List<BuildTargetData> {
    // we have to merge them, `BuildTargetData` doesn't correspond to specific provider
    // BuildTargetData` can overlap relative to source provider
    return input.groupBy { it::class }
      .map { (type, data) ->
        when {
          data.size == 1 -> data.single()
          else -> {
            @Suppress("UNCHECKED_CAST")
            val fn = mergeFunctions[type] as? MergeFunction<BuildTargetData>?
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

@ApiStatus.Internal
fun mergeFileCollections(left: SourceFileCollection, right: SourceFileCollection): SourceFileCollection {
  if (left == right) {
    return left
  }
  // merging, building trie inside another trie :p
  return object : SourceFileCollection {
    override fun isEmpty(): Boolean = left.isEmpty() && right.isEmpty()
    override fun getFiles(): Sequence<Path> = (left.getFiles() + right.getFiles()).distinct()
  }
}
