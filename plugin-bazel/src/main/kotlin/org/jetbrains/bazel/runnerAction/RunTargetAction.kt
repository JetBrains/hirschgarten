package org.jetbrains.bazel.runnerAction

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.assets.assets
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
          project.assets.presentableName,
        )
      } else {
        BspPluginBundle.message(
          "target.run.action.text",
          if (verboseText) targetInfo.buildTargetName else "",
          project.assets.presentableName,
        )
      }
    },
    isDebugAction = isDebugAction,
  )
