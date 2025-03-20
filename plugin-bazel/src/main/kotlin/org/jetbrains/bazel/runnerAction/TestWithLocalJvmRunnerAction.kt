package org.jetbrains.bazel.runnerAction

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo
import org.jetbrains.bsp.protocol.JvmEnvironmentItem
import org.jetbrains.bsp.protocol.JvmTestEnvironmentParams

class TestWithLocalJvmRunnerAction(
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
          "target.test.with.jvm.runner.action.text",
          if (includeTargetNameInText) targetInfo.buildTargetName else "",
        )
      }
    },
    isDebugMode = isDebugMode,
  ) {
  override suspend fun getEnvironment(project: Project): JvmEnvironmentItem? {
    val params = createJvmTestEnvironmentParams(targetInfo.id)
    return project.connection
      .runWithServer { it.buildTargetJvmTestEnvironment(params) }
      .items
      .firstOrNull()
  }

  private fun createJvmTestEnvironmentParams(targetId: Label) = JvmTestEnvironmentParams(listOf(targetId))
}
