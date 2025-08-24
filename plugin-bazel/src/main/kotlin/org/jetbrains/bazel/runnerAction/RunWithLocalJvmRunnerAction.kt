package org.jetbrains.bazel.runnerAction

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.sync.workspace.BazelWorkspaceResolveService
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
    return BazelWorkspaceResolveService
      .getInstance(project)
      .withEndpointProxy { it.jvmRunEnvironment(params) }
      .items
      .firstOrNull()
  }

  private fun createJvmRunEnvironmentParams(targetId: Label) = JvmRunEnvironmentParams(listOf(targetId))
}
