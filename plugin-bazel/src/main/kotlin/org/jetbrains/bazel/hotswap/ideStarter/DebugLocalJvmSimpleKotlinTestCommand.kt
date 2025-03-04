package org.jetbrains.bazel.hotswap.ideStarter

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.target.targetUtils

class DebugLocalJvmSimpleKotlinTestCommand(text: String, line: Int) : DebugLocalJvmRunnerActionCommand(text, line) {
  override suspend fun getTargetId(project: Project): Label? =
    project.targetUtils
      .allTargets()
      .firstOrNull {
        it.toString().endsWith(
          "SimpleKotlinTest",
        )
      }?.let { it }

  companion object {
    const val PREFIX = CMD_PREFIX + "debugLocalJvmSimpleKotlinTest"
  }
}
