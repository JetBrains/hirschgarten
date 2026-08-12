package org.jetbrains.bazel.sync.workspace.snapshot

import com.intellij.openapi.diagnostic.logger
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bsp.protocol.BuildTargetData
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.jetbrains.bsp.protocol.isManual
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

  fun mergeByTargetKey(targets: Collection<BuildTarget>): List<BuildTarget> =
    targets.groupBy { it.key.copy(aspectIds = WorkspaceAspectIds.EMPTY) }
      .map { (key, group) ->
        if (group.size == 1) {
          val target = group.single()
          object : BuildTarget by target {
            override val key: WorkspaceTargetKey = key
          }
        }
        else {
          // keep it deterministic
          group.sortedBy { it.key.toString() }
            .reduce { l, r -> merge(key, l, r) }
        }
      }

  private fun merge(key: WorkspaceTargetKey, left: BuildTarget, right: BuildTarget): BuildTarget {
    if (!left.isCompatibleWith(right)) {
      log.warn("Trying to merge incompatible ${BuildTarget::class}, ${key}, workspace model could be incorrect.")
    }

    return object : BuildTarget by left {
      override val key: WorkspaceTargetKey = key

      override val kind: TargetKind = left.kind.copy(languageClasses = left.kind.languageClasses + right.kind.languageClasses)

      override val dependencies: List<DependencyLabel> = (left.dependencies + right.dependencies)
        .distinctBy { it.copy(targetKey = it.targetKey.copy(aspectIds = WorkspaceAspectIds.EMPTY)) }

      override val sources: SourceFileCollection = mergeFileCollections(left.sources, right.sources)
      override val generatedSources: SourceFileCollection = mergeFileCollections(left.generatedSources, right.generatedSources)
      override val resources: SourceFileCollection = mergeFileCollections(left.resources, right.resources)

      override val data: List<BuildTargetData> = mergeBuildData(left.data.asSequence() + right.data.asSequence())
    }
  }

  private fun BuildTarget.isCompatibleWith(other: BuildTarget): Boolean =
    kind.kind == other.kind.kind
    && kind.ruleType == other.kind.ruleType
    && baseDirectory == other.baseDirectory
    && generatorName == other.generatorName
    && isManual == other.isManual
    && isWorkspace == other.isWorkspace
    && isTestOnly == other.isTestOnly

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
