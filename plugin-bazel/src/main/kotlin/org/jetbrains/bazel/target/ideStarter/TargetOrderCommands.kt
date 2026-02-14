package org.jetbrains.bazel.target.ideStarter

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import com.jetbrains.performancePlugin.CommandProvider
import com.jetbrains.performancePlugin.CreateCommand
import org.jetbrains.bazel.languages.starlark.repomapping.toShortString
import org.jetbrains.bazel.target.targetUtils
import java.nio.file.Path
import kotlin.io.path.readLines
import kotlin.io.path.writeLines

private const val SNAPSHOT_FILE_NAME = "target-order-snapshot.txt"

private fun snapshotPath(project: com.intellij.openapi.project.Project): Path {
  val basePath = checkNotNull(project.basePath) { "Project base path is null" }
  return Path.of(basePath).resolve(SNAPSHOT_FILE_NAME)
}

private fun getOrderedTargetLabels(context: PlaybackContext): List<String> {
  val project = context.project
  val targets = project.targetUtils.allBuildTargetAsLabelToTargetMap { true }
  return targets.map { it.toShortString(project) }.sorted()
}

class SaveTargetOrderCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "saveTargetOrder"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val orderedLabels = getOrderedTargetLabels(context)
    check(orderedLabels.isNotEmpty()) { "No targets found to save" }
    snapshotPath(context.project).writeLines(orderedLabels)
  }
}

class AssertTargetOrderCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "assertTargetOrder"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val currentLabels = getOrderedTargetLabels(context)
    check(currentLabels.isNotEmpty()) { "No targets found in current project" }

    val snapshotFile = snapshotPath(context.project)
    val savedLabels = snapshotFile.readLines().filter { it.isNotBlank() }
    check(savedLabels.isNotEmpty()) { "Snapshot file is empty or missing: $snapshotFile" }

    check(currentLabels.size == savedLabels.size) {
      "Target count mismatch: expected ${savedLabels.size}, got ${currentLabels.size}"
    }
    check(currentLabels == savedLabels) {
      val added = currentLabels - savedLabels.toSet()
      val removed = savedLabels - currentLabels.toSet()
      buildString {
        appendLine("Target order mismatch")
        if (added.isNotEmpty()) appendLine("  Added: $added")
        if (removed.isNotEmpty()) appendLine("  Removed: $removed")
        if (added.isEmpty() && removed.isEmpty()) appendLine("  Same targets but different order")
      }
    }
  }
}

class TargetOrderCommandProvider : CommandProvider {
  override fun getCommands(): Map<String, CreateCommand> =
    mapOf(
      SaveTargetOrderCommand.PREFIX to CreateCommand(::SaveTargetOrderCommand),
      AssertTargetOrderCommand.PREFIX to CreateCommand(::AssertTargetOrderCommand),
    )
}
