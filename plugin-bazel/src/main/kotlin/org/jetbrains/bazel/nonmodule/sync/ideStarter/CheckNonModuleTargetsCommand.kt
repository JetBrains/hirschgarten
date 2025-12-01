package org.jetbrains.bazel.nonmodule.sync.ideStarter

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.target.targetUtils

class CheckNonModuleTargetsCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  override suspend fun doExecute(context: PlaybackContext) {
    val project = context.project
    val targetUtils = project.targetUtils
    val loadedTargets = targetUtils.allTargets()
    check(loadedTargets.toSet() == setOf(Label.parse("//:bin"), Label.parse("//:test"), Label.parse("//:lib"))) {
      "Expected targets: //:bin, //:test //:lib, actual: ${loadedTargets.toSet()}"
    }

    val binInfo = checkNotNull(targetUtils.getBuildTargetForLabel(Label.parse("//:bin"))) { "No info for //:bin" }
    check(binInfo.kind.ruleType == RuleType.BINARY) {
      "Expected //:bin to be runnable, actual: ${binInfo.kind.ruleType}"
    }

    val testInfo = checkNotNull(targetUtils.getBuildTargetForLabel(Label.parse("//:test"))) { "No info for //:test" }
    check(testInfo.kind.ruleType == RuleType.TEST) {
      "Expected //:test to be testable, actual: ${testInfo.kind.ruleType}"
    }
  }

  companion object {
    const val PREFIX = "${CMD_PREFIX}checkNonModuleTargets"
  }
}
