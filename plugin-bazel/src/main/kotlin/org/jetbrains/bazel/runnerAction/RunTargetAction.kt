package org.jetbrains.bazel.runnerAction

import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.BuildTarget

open class RunTargetAction(
  project: Project,
  targetInfo: BuildTarget,
  isDebugAction: Boolean = false,
  includeTargetNameInText: Boolean = false,
) : BazelRunnerAction(
  targetInfos = listOf(targetInfo),
  text = {
    BazelRunnerActionNaming.getRunActionName(
      isDebugAction = isDebugAction,
      isRunConfigName = it,
      includeTargetNameInText = includeTargetNameInText,
      project = project,
      target = targetInfo.id,
    )
  },
  isDebugAction = isDebugAction,
)
