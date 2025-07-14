package org.jetbrains.bazel.hotswap.ideStarter

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.performanceImpl.baseCommand.DebugLocalJvmRunnerActionCommand
import org.jetbrains.bazel.target.targetUtils

class DebugLocalJvmSimpleKotlinTestCommand(text: String, line: Int) : DebugLocalJvmRunnerActionCommand(text, line) {
  override suspend fun getTargetId(project: Project): CanonicalLabel? =
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
