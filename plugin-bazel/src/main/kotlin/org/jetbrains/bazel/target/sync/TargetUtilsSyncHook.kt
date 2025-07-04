package org.jetbrains.bazel.target.sync

import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.target.sync.projectStructure.targetUtilsDiff
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.nio.file.Path

private class TargetUtilsSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val bspTargets = environment.server.workspaceBuildTargets().targets
    val targetUtilsDiff = environment.diff.targetUtilsDiff
    targetUtilsDiff.bspTargets = bspTargets
    targetUtilsDiff.fileToTarget = calculateFileToTarget(bspTargets, withLowPrioritySharedSources = true)
    targetUtilsDiff.fileToTargetWithoutLowPrioritySharedSources =
      calculateFileToTarget(bspTargets, withLowPrioritySharedSources = false)
  }

  private fun calculateFileToTarget(targets: List<BuildTarget>, withLowPrioritySharedSources: Boolean): Map<Path, List<CanonicalLabel>> {
    val resultMap = HashMap<Path, MutableList<CanonicalLabel>>()
    for (target in targets) {
      target as RawBuildTarget
      val sources =
        if (withLowPrioritySharedSources) {
          target.sources + target.lowPrioritySharedSources
        } else {
          target.sources
        }
      for (source in sources) {
        val path = source.path
        val label = target.id
        resultMap.computeIfAbsent(path) { ArrayList() }.add(label)
      }
    }
    return resultMap
  }
}
