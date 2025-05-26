package org.jetbrains.bazel.target.sync

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.target.sync.projectStructure.targetUtilsDiff
import org.jetbrains.bsp.protocol.BuildTarget
import java.nio.file.Path

private class TargetUtilsSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val bspTargets = environment.server.workspaceBuildTargets().targets
    val targetUtilsDiff = environment.diff.targetUtilsDiff
    targetUtilsDiff.bspTargets = bspTargets
    targetUtilsDiff.fileToTarget = calculateFileToTarget(bspTargets)
  }

  private fun calculateFileToTarget(targets: List<BuildTarget>): Map<Path, List<Label>> {
    val resultMap = HashMap<Path, MutableList<Label>>()
    for (target in targets) {
      for (source in target.sources) {
        val path = source.path
        val label = target.id
        resultMap.computeIfAbsent(path) { ArrayList() }.add(label)
      }
    }
    return resultMap
  }
}
