package org.jetbrains.bazel.target.sync

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.target.sync.projectStructure.targetUtilsDiff
import org.jetbrains.bsp.protocol.BuildTarget
import java.nio.file.Path
import kotlin.to

class TargetUtilsSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val bspTargets = environment.server.workspaceBuildTargets().targets
    environment.diff.targetUtilsDiff.bspTargets = bspTargets
    environment.diff.targetUtilsDiff.fileToTarget = calculateFileToTarget(bspTargets)
  }

  private fun calculateFileToTarget(targets: List<BuildTarget>): Map<Path, List<Label>> =
    targets
      .flatMap { it.toPairsPathToId() }
      .groupBy { it.first }
      .mapValues { it.value.map { pair -> pair.second } }

  private fun BuildTarget.toPairsPathToId(): List<Pair<Path, Label>> = sources.map { it.path }.map { it to id }
}
