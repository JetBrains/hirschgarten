package org.jetbrains.bazel.runnerAction

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.server.tasks.JvmRunEnvironmentTask
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bsp.protocol.JvmEnvironmentItem

class RunWithLocalJvmRunnerAction(
  targetInfo: BuildTargetInfo,
  text: (() -> String)? = null,
  isDebugMode: Boolean = false,
  includeTargetNameInText: Boolean = false,
) : LocalJvmRunnerAction(
    targetInfo = targetInfo,
    text = {
      if (text != null) {
        text()
      } else if (isDebugMode) {
        BazelPluginBundle.message(
          "target.debug.with.jvm.runner.action.text",
          if (includeTargetNameInText) targetInfo.buildTargetName else "",
        )
      } else {
        BazelPluginBundle.message(
          "target.run.with.jvm.runner.action.text",
          if (includeTargetNameInText) targetInfo.buildTargetName else "",
        )
      }
    },
    isDebugMode = isDebugMode,
  ) {
  override suspend fun getEnvironment(project: Project): JvmEnvironmentItem? =
    JvmRunEnvironmentTask(project).connectAndExecute(targetInfo.id)?.items?.first()
}
