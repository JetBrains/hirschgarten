package org.jetbrains.bazel.performanceImpl

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import com.intellij.util.io.awaitExit
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.workspace.BazelExecutableProvider

internal class RunBazelCleanCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = "${CMD_PREFIX}runBazelClean"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val project = context.project
    val bazelisk = BazelExecutableProvider.computeBazelExecutableOrFail(project)
    val rootDir = project.rootDir
    val exitCode = GeneralCommandLine(listOf(bazelisk.toAbsolutePath().toString(), "clean"))
      .withWorkingDirectory(rootDir.toNioPath())
      .withRedirectErrorStream(true)
      .createProcess()
      .awaitExit()
    check(exitCode == 0) {
      "Bazel clean failed with exit code $exitCode"
    }
  }
}
