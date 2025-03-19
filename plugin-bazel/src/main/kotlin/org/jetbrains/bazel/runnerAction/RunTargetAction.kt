package org.jetbrains.bazel.runnerAction

import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo

class RunTargetAction(
  targetInfo: BuildTargetInfo,
  isDebugAction: Boolean = false,
  includeTargetNameInText: Boolean = false,
) : BazelRunnerAction(
    targetInfos = listOf(targetInfo),
    text = { includeTargetNameInTextParam ->
      if (isDebugAction) {
        BazelPluginBundle.message(
          "target.debug.run.action.text",
          if (includeTargetNameInTextParam || includeTargetNameInText) targetInfo.buildTargetName else "",
          BazelPluginConstants.BAZEL_DISPLAY_NAME,
        )
      } else {
        BazelPluginBundle.message(
          "target.run.action.text",
          if (includeTargetNameInTextParam || includeTargetNameInText) targetInfo.buildTargetName else "",
          BazelPluginConstants.BAZEL_DISPLAY_NAME,
        )
      }
    },
    isDebugAction = isDebugAction,
  )
