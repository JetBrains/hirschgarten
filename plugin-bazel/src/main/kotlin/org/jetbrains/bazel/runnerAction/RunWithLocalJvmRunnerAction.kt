package org.jetbrains.bazel.runnerAction

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.server.connection.connection
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JvmEnvironmentItem
import org.jetbrains.bsp.protocol.JvmRunEnvironmentParams

class RunWithLocalJvmRunnerAction(
  project: Project,
  targetInfo: BuildTarget,
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
          if (includeTargetNameInText) targetInfo.id.toShortString(project) else "",
        )
      } else {
        BazelPluginBundle.message(
          "target.run.with.jvm.runner.action.text",
          if (includeTargetNameInText) targetInfo.id.toShortString(project) else "",
        )
      }
    },
    isDebugMode = isDebugMode,
  ) {
  override suspend fun getEnvironment(project: Project): JvmEnvironmentItem? {
    val params = createJvmRunEnvironmentParams(targetInfo.id)
    return project.connection
      .runWithServer { it.buildTargetJvmRunEnvironment(params) }
      .items
      .firstOrNull()
  }

  private fun createJvmRunEnvironmentParams(targetId: CanonicalLabel) = JvmRunEnvironmentParams(listOf(targetId))
}
