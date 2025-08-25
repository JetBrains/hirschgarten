package org.jetbrains.bazel.performanceImpl

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import com.intellij.openapi.vfs.readText
import com.intellij.openapi.vfs.resolveFromRootOrRelative
import org.jetbrains.bazel.config.rootDir

class AssertFileContentsEqualCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "assertFileContentsEqual"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val (expectedRelativePath, actualRelativePath) =
      try {
        extractCommandArgument(PREFIX).split(" ")
      } catch (_: Exception) {
        throw IllegalArgumentException("Usage: $PREFIX expectedRelativePath actualRelativePath")
      }
    val rootDir = context.project.rootDir
    val expectedFile = checkNotNull(rootDir.resolveFromRootOrRelative(expectedRelativePath)) { "Can't find file $expectedRelativePath" }
    val actualFile = checkNotNull(rootDir.resolveFromRootOrRelative(actualRelativePath)) { "Can't find file $actualRelativePath" }
    val expectedText = expectedFile.readText()
    val relativeText = actualFile.readText()
    check(expectedText.normalize() == relativeText.normalize()) {
      "Files don't match!\n\nExpected $expectedRelativePath:\n$expectedText\n\nActual $actualRelativePath:\n$relativeText"
    }
  }

  private fun String.normalize(): String = trim().replace("\r", "")
}
