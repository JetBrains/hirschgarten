package org.jetbrains.bazel.ui.ideStarter

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.label.CanonicalLabel
import org.jetbrains.bazel.performanceImpl.baseCommand.RunTargetActionCommand
import org.jetbrains.bazel.target.targetUtils

class RunSimpleKotlinTestCommand(text: String, line: Int) : RunTargetActionCommand(text, line) {
  override suspend fun getTargetId(project: Project): CanonicalLabel? =
    project.targetUtils
      .allTargets()
      .firstOrNull {
        it.toString().endsWith(
          "SimpleKotlinTest",
        )
      }

  companion object {
    const val PREFIX = CMD_PREFIX + "runSimpleKotlinTest"
  }
}
