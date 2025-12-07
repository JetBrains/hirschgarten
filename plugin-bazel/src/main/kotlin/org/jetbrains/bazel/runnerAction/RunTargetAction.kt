package org.jetbrains.bazel.runnerAction

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bsp.protocol.BuildTarget

class RunTargetAction(
  project: Project,
  targetInfo: BuildTarget?,
  isDebugAction: Boolean = false,
  includeTargetNameInText: Boolean = false,
) : BazelRunnerAction(
    targetInfos = listOfNotNull(targetInfo),
    text = { isRunConfigName ->
      if (isDebugAction && !isRunConfigName && !includeTargetNameInText) {
        BazelPluginBundle
          .message(
            "target.debug.run.action.text",
            "",
          ).trim()
      } else {
        BazelPluginBundle
          .message(
            "target.run.action.text",
            if (isRunConfigName || includeTargetNameInText) targetInfo?.id?.toShortString(project) ?: "" else "",
          ).trim()
      }
    },
    isDebugAction = isDebugAction,
  )
