package org.jetbrains.bazel.kotlin.sync.ideStarter

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import com.jetbrains.performancePlugin.CommandProvider
import com.jetbrains.performancePlugin.CreateCommand
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.target.targetUtils

class CheckTargetsInTargetWidget(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  override suspend fun doExecute(context: PlaybackContext) {
    val project = context.project
    val loadedTargets = project.targetUtils.allTargets().toSet()
    val expectedTargets =
      setOf(
        Label.parse("//java:binary"),
        Label.parse("//java:library"),
        Label.parse("//java:test"),
        Label.parse("//kotlin:binary"),
        Label.parse("//kotlin:library"),
        Label.parse("//kotlin:test"),
      )
    val missingTargets = expectedTargets - loadedTargets
    check(missingTargets.isEmpty()) { "Missing expected targets: $missingTargets (loaded ${loadedTargets.size} targets)" }
  }

  companion object {
    const val PREFIX = "${CMD_PREFIX}checkTargetsInTargetWidget"
  }
}

class CheckTargetsInTargetWidgetProvider : CommandProvider {
  override fun getCommands(): Map<String, CreateCommand?> =
    mapOf(
      CheckTargetsInTargetWidget.PREFIX to CreateCommand(::CheckTargetsInTargetWidget),
    )
}
