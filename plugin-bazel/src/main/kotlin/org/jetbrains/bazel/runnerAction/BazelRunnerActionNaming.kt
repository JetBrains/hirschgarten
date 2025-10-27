package org.jetbrains.bazel.runnerAction

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString

object BazelRunnerActionNaming {

  @JvmStatic
  fun getRunActionName(
    isDebugAction: Boolean,
    isRunConfigName: Boolean,
    includeTargetNameInText: Boolean,
    project: Project,
    target: Label,
  ): String {
    return if (isDebugAction && !isRunConfigName && !includeTargetNameInText) {
      BazelPluginBundle
        .message(
          "target.debug.run.action.text",
          "",
        ).trim()
    } else {
      BazelPluginBundle
        .message(
          "target.run.action.text",
          if (isRunConfigName || includeTargetNameInText) target.toShortString(project) else "",
        ).trim()
    }
  }
}
