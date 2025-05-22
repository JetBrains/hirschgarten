package org.jetbrains.bazel.runnerAction

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bsp.protocol.BuildTarget

class RunTargetAction(
  project: Project,
  targetInfo: BuildTarget,
  isDebugAction: Boolean = false,
  includeTargetNameInText: Boolean = false,
) : BazelRunnerAction(
    targetInfos = listOf(targetInfo),
    text = { includeTargetNameInTextParam ->
      if (isDebugAction) {
        BazelPluginBundle.message(
          "target.debug.run.action.text",
          if (includeTargetNameInTextParam || includeTargetNameInText) targetInfo.id.toShortString(project) else "",
        )
      } else {
        BazelPluginBundle.message(
          "target.run.action.text",
          if (includeTargetNameInTextParam || includeTargetNameInText) targetInfo.id.toShortString(project) else "",
        )
      }
    },
    isDebugAction = isDebugAction,
  )
