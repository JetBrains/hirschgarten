package org.jetbrains.bazel.runnerAction

import ch.epfl.scala.bsp4j.JvmEnvironmentItem
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BspPluginBundle
import org.jetbrains.bazel.server.tasks.JvmRunEnvironmentTask
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo

class RunWithLocalJvmRunnerAction(
  targetInfo: BuildTargetInfo,
  text: (() -> String)? = null,
  isDebugMode: Boolean = false,
  verboseText: Boolean = false,
) : LocalJvmRunnerAction(
    targetInfo = targetInfo,
    text = {
      if (text != null) {
        text()
      } else if (isDebugMode) {
        BspPluginBundle.message(
          "target.debug.with.jvm.runner.action.text",
          if (verboseText) targetInfo.buildTargetName else "",
        )
      } else {
        BspPluginBundle.message(
          "target.run.with.jvm.runner.action.text",
          if (verboseText) targetInfo.buildTargetName else "",
        )
      }
    },
    isDebugMode = isDebugMode,
  ) {
  override suspend fun getEnvironment(project: Project): JvmEnvironmentItem? =
    JvmRunEnvironmentTask(project).connectAndExecute(targetInfo.id)?.items?.first()
}
