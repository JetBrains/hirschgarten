package org.jetbrains.plugins.bsp.ui.console

import com.google.common.base.Throwables.getStackTraceAsString
import com.intellij.build.BuildProgressListener
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.FilePosition
import com.intellij.build.events.EventResult
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.FileMessageEventImpl
import com.intellij.build.events.impl.FinishBuildEventImpl
import com.intellij.build.events.impl.FinishEventImpl
import com.intellij.build.events.impl.OutputBuildEventImpl
import com.intellij.build.events.impl.ProgressBuildEventImpl
import com.intellij.build.events.impl.StartBuildEventImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.action.SuspendableAction
import org.jetbrains.plugins.bsp.building.action.isBuildInProgress
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.config.BspPluginIcons
import org.jetbrains.plugins.bsp.impl.actions.registered.ResyncAction
import org.jetbrains.plugins.bsp.impl.actions.registered.isSyncInProgress
import org.jetbrains.plugins.bsp.impl.projectAware.BspWorkspaceListener
import java.io.File
import java.net.URI

private data class SubtaskParents(val rootTask: Any, val parentTask: Any)

private val log = logger<TaskConsole>()

public abstract class TaskConsole(
  private val taskView: BuildProgressListener,
  private val basePath: String,
  private val buildToolName: String,
  private val project: Project,
) {
  protected val tasksInProgress: MutableList<Any> = mutableListOf()
  private val subtaskParentMap: LinkedHashMap<Any, SubtaskParents> = linkedMapOf()

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
  @Synchronized
  public fun startTask(
    taskId: Any,
    title: String,
    message: String,
    cancelAction: () -> Unit = {},
    redoAction: (() -> Unit)? = null,
  ): Unit =
    doUnlessTaskInProgress(taskId) {
      tasksInProgress.add(taskId)
      doStartTask(
        taskId,
        BspPluginBundle.message("console.tasks.title", buildToolName, title),
        message,
        cancelAction,
        redoAction,
      )
    }

  private fun doStartTask(
    taskId: Any,
    title: String,
    message: String,
    cancelAction: () -> Unit,
    redoAction: (() -> Unit)?,
  ) {
    val taskDescriptor = DefaultBuildDescriptor(taskId, title, basePath, System.currentTimeMillis())
    taskDescriptor.isActivateToolWindowWhenAdded = true
    addRedoActionToTheDescriptor(taskDescriptor, redoAction)
    addCancelActionToTheDescriptor(taskId, taskDescriptor, cancelAction)

    val startEvent = StartBuildEventImpl(taskDescriptor, message)
    taskView.onEvent(taskId, startEvent)
  }

  private fun addCancelActionToTheDescriptor(
    taskId: Any,
    taskDescriptor: DefaultBuildDescriptor,
    doCancelAction: () -> Unit,
  ) {
    val cancelAction = CancelAction(doCancelAction, taskId)
    taskDescriptor.withAction(cancelAction)
  }

  private fun addRedoActionToTheDescriptor(taskDescriptor: DefaultBuildDescriptor, redoAction: (() -> Unit)? = null) {
    val action = calculateRedoAction(redoAction)
    taskDescriptor.withAction(action)
  }

  protected abstract fun calculateRedoAction(redoAction: (() -> Unit)?): AnAction

  public fun hasTasksInProgress(): Boolean = tasksInProgress.isNotEmpty()

  /**
   * Displays finish of a task (and all its children) in this console.
   * Will not display anything if a task with given `taskId` is not running

   * @param taskId ID of the finished task (last started task by default, if nothing passed)
   * @param message message informing about the start of the task
   * @param result result of the task execution (success by default, if nothing passed)
   */
  @Synchronized
  public fun finishTask(
    taskId: Any,
    message: String,
    result: EventResult = SuccessResultImpl(),
  ): Unit =
    doIfTaskInProgress(taskId) {
      doFinishTask(taskId, message, result)
    }

  private fun doFinishTask(
    taskId: Any,
    message: String,
    result: EventResult,
  ) {
    if (result is FailureResultImpl) {
      result.failures.forEach {
        log.error(it.message, it.error)
        it.error?.let { error ->
          addMessage(taskId, getStackTraceAsString(error))
        }
      }
    }
    tasksInProgress.remove(taskId)
    subtaskParentMap.entries.removeAll { it.value.rootTask == taskId }
    val event = FinishBuildEventImpl(taskId, null, System.currentTimeMillis(), message, result)
    taskView.onEvent(taskId, event)
  }

  @Synchronized
  public fun finishAllTasks(message: String, result: EventResult = SuccessResultImpl()) {
  }

  /**
   * Displays start of a subtask in this console
   *
   * @param parentTaskId id of the task (or another subtask) being this subtask's direct parent
   * @param subtaskId id of the newly created subtask. **Has to be unique among all running subtasks in
   * this console - otherwise, unexpected behavior might occur**
   * @param message will be displayed as this subtask's title until it's finished
   */
  @Synchronized
  public fun startSubtask(
    parentTaskId: Any,
    subtaskId: Any,
    message: String,
  ) {
    val rootTaskId = subtaskParentMap[parentTaskId]?.rootTask ?: parentTaskId
    doIfTaskInProgress(rootTaskId) {
      doStartSubtask(rootTaskId, parentTaskId, subtaskId, message)
    }
  }

  private fun doStartSubtask(
    rootTaskId: Any,
    parentTaskId: Any,
    subtaskId: Any,
    message: String,
  ) {
    subtaskParentMap[subtaskId] =
      SubtaskParents(
        rootTask = rootTaskId,
        parentTask = parentTaskId,
      )
    val event = ProgressBuildEventImpl(subtaskId, parentTaskId, System.currentTimeMillis(), message, -1, -1, "")
    taskView.onEvent(rootTaskId, event)
  }

  /**
   * Displays finishing of a subtask in this console
   *
   * @param subtaskId id of the subtask to be finished.
   * If there is no such subtask running, this method will not do anything
   * @param message will be displayed as this subtask's title after it is finished
   * @param result result type of the subtask, default [SuccessResultImpl]
   */
  @Synchronized
  public fun finishSubtask(
    subtaskId: Any,
    message: String,
    result: EventResult = SuccessResultImpl(),
  ) {
    subtaskParentMap[subtaskId]?.let {
      doIfTaskInProgress(it.rootTask) {
        doFinishSubtask(it.rootTask, subtaskId, message, result)
      }
    }
  }

  private fun doFinishSubtask(
    rootTask: Any,
    subtaskId: Any,
    message: String,
    result: EventResult,
  ) {
    subtaskParentMap.remove(subtaskId)
    finishAllDescendants(subtaskId)
    val event = FinishEventImpl(subtaskId, null, System.currentTimeMillis(), message, result)
    taskView.onEvent(rootTask, event)
  }

  private fun finishAllDescendants(parentId: Any) {
    subtaskParentMap
      .filterValues { it.parentTask == parentId }
      .keys
      .forEach {
        subtaskParentMap.remove(it)
        finishAllDescendants(it)
      }
  }

  /**
   * Adds a diagnostic message to a particular task in this console.
   *
   * @param taskId id of the task (or a subtask), to which the message will be added
   * @param fileURI absolute path to the file concerned by the diagnostic
   * @param line line number in given file (first line is 0)
   * @param column column number in given file (first column is 0)
   * @param message description of the diagnostic
   * @param severity severity of the diagnostic
   */
  @Synchronized
  public fun addDiagnosticMessage(
    taskId: Any,
    fileURI: String,
    line: Int,
    column: Int,
    message: String,
    severity: MessageEvent.Kind,
  ) {
    maybeGetRootTask(taskId)?.let {
      doIfTaskInProgress(it) {
        if (message.isNotBlank()) {
          val fullFileURI = if (fileURI.startsWith("file://")) fileURI else "file://$fileURI"
          val subtaskId =
            if (tasksInProgress.contains(taskId)) it else taskId
          val filePosition = FilePosition(File(URI(fullFileURI)), line, column)
          doAddDiagnosticMessage(it, subtaskId, filePosition, message, severity)
        }
      }
    }
  }

  private fun doAddDiagnosticMessage(
    taskId: Any,
    subtaskId: Any,
    filePosition: FilePosition,
    message: String,
    severity: MessageEvent.Kind,
  ) {
    val event =
      FileMessageEventImpl(
        subtaskId,
        severity,
        null,
        prepareTextToPrint(message),
        null,
        filePosition,
      )
    taskView.onEvent(taskId, event)
  }

  /**
   * Adds a message to the latest task in this console.
   */
  @Synchronized
  public fun addMessage(message: String) {
    val entry = subtaskParentMap.entries.lastOrNull()
    val taskId = tasksInProgress.lastOrNull()
    entry?.let { addMessage(it.key, message) } ?: taskId?.let { addMessage(taskId, message) }
  }

  /**
   * Adds a message to a particular task in this console. If the message is added to a subtask, it will also be
   * added to the subtask's parent task.
   *
   * @param taskId id of the task (or a subtask), to which the message will be added
   * @param message message to be added. New line will be inserted at its end if it's not present there already
   */
  @Synchronized
  public fun addMessage(taskId: Any, message: String) {
    maybeGetRootTask(taskId)?.let {
      doIfTaskInProgress(it) {
        if (message.isNotBlank()) {
          doAddMessage(taskId, message)
        }
      }
    }
  }

  private fun doAddMessage(taskId: Any, message: String) {
    val messageToSend = prepareTextToPrint(message)
    sendMessageEvent(taskId, messageToSend)
    sendMessageToAncestors(taskId, messageToSend)
  }

  private fun sendMessageToAncestors(taskId: Any, message: String): Unit =
    doUnlessTaskInProgress(taskId) {
      // if taskID is a root task, it has no parents
      maybeGetParentTask(taskId)?.let {
        if (it != taskId) {
          sendMessageEvent(it, message)
          sendMessageToAncestors(it, message)
        }
      }
    }

  private fun sendMessageEvent(taskId: Any, message: String) {
    val subtaskId =
      if (tasksInProgress.contains(taskId)) null else taskId
    val event = OutputBuildEventImpl(subtaskId, message, true)
    maybeGetRootTask(taskId)?.let { taskView.onEvent(it, event) }
  }

  private inline fun doIfTaskInProgress(taskId: Any, action: () -> Unit) {
    if (tasksInProgress.contains(taskId)) {
      action()
    }
  }

  private inline fun doUnlessTaskInProgress(taskId: Any, action: () -> Unit) {
    if (!tasksInProgress.contains(taskId)) {
      action()
    }
  }

  private fun prepareTextToPrint(text: String): String = if (text.endsWith("\n")) text else text + "\n"

  private fun maybeGetParentTask(taskId: Any): Any? = if (tasksInProgress.contains(taskId)) taskId else subtaskParentMap[taskId]?.parentTask

  private fun maybeGetRootTask(taskId: Any): Any? = if (tasksInProgress.contains(taskId)) taskId else subtaskParentMap[taskId]?.rootTask

  private inner class CancelAction(private val doCancelAction: () -> Unit, private val taskId: Any) :
    DumbAwareAction({ "Stop" }, BspPluginIcons.disconnect) {
    init {
      project.messageBus.connect().subscribe(
        BspWorkspaceListener.TOPIC,
        object : BspWorkspaceListener {
          override fun syncStarted() {}

          override fun syncFinished(canceled: Boolean) {}

          override fun allTasksCancelled() {
            doCancelAction()
          }
        },
      )
    }

    override fun actionPerformed(e: AnActionEvent) {
      doCancelAction()
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = tasksInProgress.contains(taskId)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
  }
}

public class SyncTaskConsole(
  taskView: BuildProgressListener,
  basePath: String,
  buildToolName: String,
  project: Project,
) : TaskConsole(taskView, basePath, buildToolName, project) {
  override fun calculateRedoAction(redoAction: (() -> Unit)?): AnAction =
    object : SuspendableAction({ BspPluginBundle.message("resync.action.text") }, BspPluginIcons.reload) {
      override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
        redoAction?.invoke() ?: ResyncAction().actionPerformed(e)
      }

      override fun update(project: Project, e: AnActionEvent) {
        e.presentation.isEnabled = !project.isSyncInProgress() && !project.isBuildInProgress()
      }
    }
}

public class BuildTaskConsole(
  taskView: BuildProgressListener,
  basePath: String,
  buildToolName: String,
  project: Project,
) : TaskConsole(taskView, basePath, buildToolName, project) {
  override fun calculateRedoAction(redoAction: (() -> Unit)?): AnAction =
    object : AnAction({ BspPluginBundle.message("rebuild.action.text") }, AllIcons.Actions.Compile) {
      override fun actionPerformed(e: AnActionEvent) {
        redoAction?.invoke()
      }

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = redoAction != null && tasksInProgress.isEmpty()
      }

      override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    }
}
