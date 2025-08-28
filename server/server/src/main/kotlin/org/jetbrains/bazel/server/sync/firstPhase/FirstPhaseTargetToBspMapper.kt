package org.jetbrains.bazel.server.sync.firstPhase

import org.jetbrains.bazel.server.model.PhasedSyncProject
import org.jetbrains.bsp.protocol.RawPhasedTarget
import org.jetbrains.bsp.protocol.WorkspacePhasedBuildTargetsResult

class FirstPhaseTargetToBspMapper {
  fun toWorkspaceBuildTargetsResult(project: PhasedSyncProject): WorkspacePhasedBuildTargetsResult {
    val targets =
      project.modules
        .mapValues { RawPhasedTarget(it.value) }
    return WorkspacePhasedBuildTargetsResult(targets)
  }
}
