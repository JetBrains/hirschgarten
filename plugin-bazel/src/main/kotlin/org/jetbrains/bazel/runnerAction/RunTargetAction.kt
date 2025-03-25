package org.jetbrains.bazel.runnerAction

import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bsp.protocol.BuildTarget

class RunTargetAction(
  targetInfo: BuildTarget,
  isDebugAction: Boolean = false,
  includeTargetNameInText: Boolean = false,
) : BazelRunnerAction(
    targetInfos = listOf(targetInfo),
    text = { includeTargetNameInTextParam ->
      if (isDebugAction) {
        BazelPluginBundle.message(
          "target.debug.run.action.text",
          if (includeTargetNameInTextParam || includeTargetNameInText) targetInfo.displayName else "",
          BazelPluginConstants.BAZEL_DISPLAY_NAME,
        )
      } else {
        BazelPluginBundle.message(
          "target.run.action.text",
          if (includeTargetNameInTextParam || includeTargetNameInText) targetInfo.displayName else "",
          BazelPluginConstants.BAZEL_DISPLAY_NAME,
        )
      }
    },
    isDebugAction = isDebugAction,
  )
