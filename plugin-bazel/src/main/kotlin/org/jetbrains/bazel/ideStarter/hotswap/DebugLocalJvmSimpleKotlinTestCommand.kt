package org.jetbrains.bazel.ideStarter.hotSwap

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.ideStarter.common.DebugLocalJvmRunnerActionCommand
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils

class DebugLocalJvmSimpleKotlinTestCommand(text: String, line: Int) : DebugLocalJvmRunnerActionCommand(text, line) {
  override suspend fun getTargetId(project: Project): BuildTargetIdentifier? =
    project.temporaryTargetUtils.allTargetIds().firstOrNull { it.uri.endsWith("SimpleKotlinTest") }

  companion object {
    const val PREFIX = CMD_PREFIX + "debugLocalJvmSimpleKotlinTest"
  }
}
