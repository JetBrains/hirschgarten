package org.jetbrains.bazel.progress

import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.diagnostic.rethrowControlFlowException
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.platform.util.progress.SequentialProgressReporter
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelExtBundle
import org.jetbrains.bsp.protocol.TaskId
import java.util.concurrent.TimeoutException

private val log = logger<TaskConsole>()

val Project.syncConsole: TaskConsole
  @ApiStatus.Internal
  get() = service<ConsoleService>().syncConsole

/**
 * Runs [action], which calls [withSubtask], so that its failure does not fail the parent task with [taskId].
 * The failure stops here, so this function logs it.
 * It also reports the failure as a diagnostic message of the parent task. The console skips it when a subtask already shows it.
 * It rethrows a control-flow exception and a cancellation.
 */
@ApiStatus.Internal
suspend fun <T> TaskConsole.isolateSubtaskFailure(taskId: TaskId, action: suspend () -> T): T? = try {
  action()
}
catch (e: Throwable) {
  rethrowControlFlowException(e)
  addDiagnosticMessage(
    taskId = taskId,
    message = e.message ?: "Unknown error",
    severity = MessageEvent.Kind.ERROR,
    error = e,
  )
  log.error(e)
  null
}

/**
 * Runs [block] as a subtask with [subtaskId]. On a failure, it shows the failure and rethrows the original exception.
 * The console shows each exception one time, so the enclosing subtasks only show that they failed.
 * A failed subtask fails its parent task. To continue after a failure, use [isolateSubtaskFailure].
 */
@ApiStatus.Internal
suspend fun <T> TaskConsole.withSubtask(
  subtaskId: TaskId,
  @NlsContexts.ProgressTitle message: String,
  block: suspend (subtaskId: TaskId) -> T,
): T {
  startSubtask(subtaskId, message)
  try {
    val result = block(subtaskId)
    finishSubtask(subtaskId)
    return result
  }
  catch (ex: Throwable) {
    val result = when (ex) {
      is java.util.concurrent.CancellationException -> FailureResultImpl(BazelExtBundle.message("console.task.exception.cancellation.message"), ex)
      is ControlFlowException -> FailureResultImpl()
      is TimeoutException -> FailureResultImpl(BazelExtBundle.message("console.task.exception.timeout.message"), ex)
      else -> FailureResultImpl(ex)
    }
    finishSubtask(subtaskId = subtaskId, result = result)
    throw ex
  }
}

@ApiStatus.Internal
suspend fun <T> TaskConsole.withSubtask(
  reporter: SequentialProgressReporter,
  subtaskId: TaskId,
  text: String,
  block: suspend (subtaskId: TaskId) -> T,
): T {
  return reporter.indeterminateStep(text) {
    withSubtask(
      subtaskId = subtaskId,
      message = text,
      block = block,
    )
  }
}
