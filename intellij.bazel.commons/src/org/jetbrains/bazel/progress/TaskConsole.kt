package org.jetbrains.bazel.progress

import com.intellij.build.events.EventResult
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.SuccessResultImpl
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path

@ApiStatus.Internal
interface TaskConsole {
  /**
   * Displays start of a task in this console.
   * Will not display anything if a task with given `taskId` is already running
   *
   * @param taskId ID of the newly started task
   * @param title task title which will be displayed in the console
   * @param message message informing about the start of the task
   * @param cancelAction action which will be executed on cancel button click
   * @param redoAction action which will be executed on redo button click
   */
  fun startTask(
    taskId: TaskId,
    title: String,
    message: String,
    cancelAction: () -> Unit = {},
    redoAction: (suspend () -> Unit)? = null,
    showConsole: ShowConsole = ShowConsole.ALWAYS,
  )

  fun hasTasksInProgress(): Boolean

  /**
   * Displays finish of a task (and all its children) in this console.
   * Will not display anything if a task with given `taskId` is not running

   * @param taskId ID of the finished task (last started task by default, if nothing passed)
   * @param message message informing about the start of the task
   * @param result result of the task execution (success by default, if nothing passed)
   */
  fun finishTask(
    taskId: TaskId,
    message: String,
    result: EventResult = SuccessResultImpl(),
  )

  /**
   * Displays start of a subtask in this console
   *
   * @param parentTaskId id of the task (or another subtask) being this subtask's direct parent
   * @param subtaskId id of the newly created subtask. **Has to be unique among all running subtasks in
   * this console - otherwise, unexpected behavior might occur**
   * @param message will be displayed as this subtask's title until it's finished
   */
  fun startSubtask(
    subtaskId: TaskId,
    message: String,
  )

  /**
   * Displays finishing of a subtask in this console
   *
   * @param subtaskId id of the subtask to be finished.
   * If there is no such subtask running, this method will not do anything
   * @param result result type of the subtask, default [SuccessResultImpl]
   */
  fun finishSubtask(
    subtaskId: TaskId,
    message: String? = null,
    result: EventResult = SuccessResultImpl(),
  )

  /**
   * Adds a diagnostic message to a particular task in this console.
   *
   * @param taskId id of the task (or a subtask), to which the message will be added
   * @param path absolute path to the file concerned by the diagnostic
   * @param line line number in given file (first line is 0)
   * @param column column number in given file (first column is 0)
   * @param message description of the diagnostic
   * @param severity severity of the diagnostic
   */
  fun addDiagnosticMessage(
    taskId: TaskId,
    path: Path?,
    line: Int,
    column: Int,
    message: String,
    severity: MessageEvent.Kind,
  )

  /**
   * Adds a message to a particular task in this console. If the message is added to a subtask, it will also be
   * added to the subtask's parent task.
   *
   * @param taskId id of the task (or a subtask), to which the message will be added
   * @param message message to be added. New line will be inserted at its end if it's not present there already
   */
  fun addMessage(taskId: TaskId, message: String)

  /**
   * Registers new exception during task processing.
   * NOTE: this does not add any UI elements. It is to prevent duplicate UI nodes when subtask fails
   *
   * @param taskId id of the task (or a subtask) whic produced an exception
   * @param ex exception itself
   * @return true if this exception is new and was not registered already
   */
  fun registerException(taskId: TaskId, ex: Throwable): Boolean
}
