package org.jetbrains.bazel.hotswap.ideStarter

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bsp.protocol.BuildTargetIdentifier

class DebugLocalJvmSimpleKotlinTestCommand(text: String, line: Int) : DebugLocalJvmRunnerActionCommand(text, line) {
  override suspend fun getTargetId(project: Project): BuildTargetIdentifier? =
    project.targetUtils
      .allTargets()
      .firstOrNull {
        it.toString().endsWith(
          "SimpleKotlinTest",
        )
      }?.let { BuildTargetIdentifier(it.toString()) }

  companion object {
    const val PREFIX = CMD_PREFIX + "debugLocalJvmSimpleKotlinTest"
  }
}
