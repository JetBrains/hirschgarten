package org.jetbrains.bazel.sync.workspace.snapshot

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.id

@ApiStatus.Internal
interface ExecutableTargetsIndex {
  fun executableTargetsFor(label: Label): List<Label>

  companion object {
    val EMPTY: ExecutableTargetsIndex = object : ExecutableTargetsIndex {
      override fun executableTargetsFor(label: Label): List<Label> = listOf()
    }
  }
}

@ApiStatus.Internal
class InMemoryExecutableTargetsIndex internal constructor(
  private val hash2Keys: Map<Label, List<Label>>,
) : ExecutableTargetsIndex {
  override fun executableTargetsFor(label: Label): List<Label> = hash2Keys[label] ?: listOf()
}

@ApiStatus.Internal
object ExecutableTargetsIndexBuilder {
  fun build(
    targetGraph: WorkspaceTargetGraph,
    importDepth: Int,
    targets: Collection<BuildTarget>,
  ): ExecutableTargetsIndex = InMemoryExecutableTargetsIndex(buildExecutableTargetsIndex(targetGraph, importDepth, targets))

  internal fun buildExecutableTargetsIndex(
    targetGraph: WorkspaceTargetGraph,
    importDepth: Int,
    targets: Collection<BuildTarget>,
  ): Map<Label, List<Label>> {
    val byKey = HashMap<WorkspaceTargetKey, BuildTarget>(targets.size)
    for (target in targets) {
      byKey[target.key] = target
    }
    val imported = targetGraph.findAllTargetsAtDepth(maxDepth = importDepth, useRelaxedDependencyExpansion = true)
      .mapNotNull { byKey[it.targetKey] }
      .toList()
    val labelToTarget = imported.associateByTo(HashMap(imported.size)) { it.id }
    return ExecutableTargetsComputer.calculateExecutableTargets(targets = imported, labelToTargetInfo = labelToTarget)
      .mapKeys { (k, _) -> k as Label }
  }
}
