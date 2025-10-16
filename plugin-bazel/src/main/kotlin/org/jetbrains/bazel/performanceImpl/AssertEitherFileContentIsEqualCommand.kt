package org.jetbrains.bazel.performanceImpl

import com.intellij.openapi.ui.playback.PlaybackContext
import com.intellij.openapi.ui.playback.commands.PlaybackCommandCoroutineAdapter
import com.intellij.openapi.vfs.readText
import org.jetbrains.bazel.config.rootDir

class AssertEitherFileContentIsEqualCommand(text: String, line: Int) : PlaybackCommandCoroutineAdapter(text, line) {
  companion object {
    const val PREFIX = CMD_PREFIX + "assertEitherContentsEqual"
  }

  override suspend fun doExecute(context: PlaybackContext) {
    val args =
      try {
        extractCommandArgument(PREFIX).split(" ")
      } catch (_: Exception) {
        throw IllegalArgumentException("Usage: $PREFIX expectedRelativePath actualRelativePath")
      }
    val actualRelativePath = args[0]
    val rootDir = context.project.rootDir
    val actualFile = checkNotNull(rootDir.resolveFromRelativeOrRoot(actualRelativePath)) { "Can't find file $actualRelativePath" }
    val relativeText = actualFile.readText()
    var isEqual = false
    for (expectedRelativePath in args.subList(1, args.size)) {
      val expectedFile = checkNotNull(rootDir.resolveFromRelativeOrRoot(expectedRelativePath)) { "Can't find file $expectedRelativePath" }
      val expectedText = expectedFile.readText()
      if (expectedText.normalize() == relativeText.normalize()) {
        isEqual = true
        break
      }
    }
    check(isEqual) {
      val expectedContents = buildString {
        append("Files don't match!\n\nExpected one of:\n")
        for (path in args.subList(1, args.size)) {
          val content = context.project.rootDir.resolveFromRelativeOrRoot(path)?.readText()?.normalize() ?: "<file not found>"
          append("$path:\n$content\n\n")
        }
        append("Actual $actualRelativePath:\n$relativeText")
      }
      expectedContents
    }
  }

  private fun String.normalize(): String = trim().replace("\r", "")
}
