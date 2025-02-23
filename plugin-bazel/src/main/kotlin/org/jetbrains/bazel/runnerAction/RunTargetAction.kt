package org.jetbrains.bazel.runnerAction

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo

class RunTargetAction(
  targetInfo: BuildTargetInfo,
  text: (() -> String)? = null,
  isDebugAction: Boolean = false,
  verboseText: Boolean = false,
  project: Project,
) : BspRunnerAction(
    targetInfos = listOf(targetInfo),
    text = {
      if (text != null) {
        text()
      } else if (isDebugAction) {
        BspPluginBundle.message(
          "target.debug.run.action.text",
          if (verboseText) targetInfo.buildTargetName else "",
          BazelPluginConstants.BAZEL_DISPLAY_NAME,
        )
      } else {
        BspPluginBundle.message(
          "target.run.action.text",
          if (verboseText) targetInfo.buildTargetName else "",
          BazelPluginConstants.BAZEL_DISPLAY_NAME,
        )
      }
    },
    isDebugAction = isDebugAction,
  )
