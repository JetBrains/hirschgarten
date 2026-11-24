package org.jetbrains.bazel.run.task

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
class BazelTestTaskListener(handler: ProcessHandler, private val coverageReportListener: ((Path) -> Unit)? = null) :
  BazelRunTaskListener(handler) {
  override fun onTestConsoleOutput(text: String, isError: Boolean) {
    handler.notifyTextAvailable(text.toStringWithNewline(), if (isError) ProcessOutputType.STDERR else ProcessOutputType.STDOUT)
  }

  override fun onPublishCoverageReport(coverageReport: Path) {
    coverageReportListener?.invoke(coverageReport)
  }
}

/**
 * If a system message sent to the process handler does not end with a newline,
 * it might connect to another message and not be parsed correctly
 * */
private fun String.toStringWithNewline(): String = if (this.endsWith("\n")) this else "$this\n"
