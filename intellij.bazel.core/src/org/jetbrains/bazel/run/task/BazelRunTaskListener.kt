package org.jetbrains.bazel.run.task

import com.intellij.build.events.MessageEvent
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessOutputType
import com.intellij.openapi.util.Key
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.progress.ConsoleService
import org.jetbrains.bazel.run.BazelProcessHandler
import org.jetbrains.bazel.taskEvents.BazelTaskListener
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path

/**
 * Shows the output of Bazel in the console of the [handler].
 * Sends the diagnostics of the build of a `bazel run/test` to the build console, as `bazel build` does.
 */
@ApiStatus.Internal
open class BazelRunTaskListener(protected val handler: ProcessHandler) : BazelTaskListener {
  private val usePty: Boolean = (handler as? BazelProcessHandler)?.usePty == true
  private val ansiEscapeDecoder = AnsiEscapeDecoder()
  private val buildListener: BazelBuildTaskListener? =
    (handler as? BazelProcessHandler)?.let { BazelBuildTaskListener(ConsoleService.getInstance(it.project).buildConsole) }

  override fun onDiagnostic(
    taskId: TaskId,
    textDocument: Path?,
    buildTarget: Label,
    line: Int,
    character: Int,
    severity: MessageEvent.Kind,
    message: String,
    description: String?,
  ) {
    buildListener?.onDiagnostic(taskId, textDocument, buildTarget, line, character, severity, message, description)
  }

  override fun onTaskFinish(taskId: TaskId, message: String, status: BazelStatus, data: Any?) {
    buildListener?.onTaskFinish(taskId, message, status, data)
  }

  override fun onOutputStream(taskId: TaskId?, text: String) {
    notifyBazelOutput(text, ProcessOutputType.STDOUT)
  }

  override fun onErrorStream(taskId: TaskId?, text: String) {
    notifyBazelOutput(text, ProcessOutputType.STDERR)
  }

  // For compatibility with older BSP servers and JB test runner
  // TODO: Log messages in the correct place
  override fun onLogMessage(taskId: TaskId, message: String) {
    notifyBazelOutput(message, ProcessOutputType.STDOUT)
  }

  private fun notifyBazelOutput(text: String, outputType: ProcessOutputType) {
    if (usePty) {
      handler.notifyTextAvailable(text, outputType)
    }
    else {
      ansiEscapeDecoder.escapeText(text, outputType) { s: String, key: Key<Any> -> handler.notifyTextAvailable(s, key) }
    }
  }
}
