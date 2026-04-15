package org.jetbrains.bazel.ui.console

import com.google.common.base.Throwables.getStackTraceAsString
import com.intellij.build.BuildContentDescriptor
import com.intellij.build.BuildProgressListener
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.FilePosition
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.EventResult
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.OutputBuildEvent
import com.intellij.build.events.StartBuildEvent
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.FileMessageEventImpl
import com.intellij.build.events.impl.FinishBuildEventImpl
import com.intellij.build.events.impl.FinishEventImpl
import com.intellij.build.events.impl.MessageEventImpl
import com.intellij.build.events.impl.OutputBuildEventImpl
import com.intellij.build.events.impl.ProgressBuildEventImpl
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.terminal.TerminalExecutionConsole
import com.intellij.terminal.TerminalExecutionConsoleBuilder
import com.jediterm.core.util.TermSize
import com.jediterm.terminal.TtyConnector
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.BazelPluginConstants
import org.jetbrains.bazel.progress.ConsoleService
import org.jetbrains.bazel.progress.PtyAwareTaskConsole
import org.jetbrains.bazel.progress.ShowConsole
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bazel.sync.status.SyncStatusListener
import org.jetbrains.bazel.sync.status.isSyncInProgress
import org.jetbrains.bsp.protocol.TaskId
import java.nio.file.Path

@ApiStatus.Internal
abstract class BaseTaskConsole(
  private val taskView: BuildProgressListener,
  private val basePath: String,
  private val project: Project,
) : TaskConsole, PtyAwareTaskConsole {
  companion object {
    private val log = logger<BaseTaskConsole>()
  }

  protected val tasksInProgress: MutableSet<TaskId> = mutableSetOf()
  private val taskFailures: MutableMap<TaskId, MutableSet<Throwable>> = linkedMapOf()
  private val subtaskParentMap: MutableMap<TaskId, TaskId> = linkedMapOf()
  private val subtaskMessageMap: MutableMap<TaskId, String> = linkedMapOf()
  private val taskPtyTerminalMap: MutableMap<TaskId, TerminalExecutionConsole> = linkedMapOf()

  @Synchronized
  override fun startTask(
    taskId: TaskId,
    title: String,
    message: String,
    cancelAction: () -> Unit,
    redoAction: (suspend () -> Unit)?,
    showConsole: ShowConsole,
  ) {
    if (tasksInProgress.add(taskId)) {
      doStartTask(
        taskId,
        BazelPluginBundle.message("console.tasks.title", BazelPluginConstants.BAZEL_DISPLAY_NAME, title),
        message,
        cancelAction,
        redoAction,
        showConsole,
      )
    }
  }

  private fun doStartTask(
    taskId: TaskId,
    title: String,
    message: String,
    cancelAction: () -> Unit,
    redoAction: (suspend () -> Unit)?,
    showConsole: ShowConsole,
  ) {
    val taskDescriptor = DefaultBuildDescriptor(taskId, taskId.taskGroupId, title, basePath, System.currentTimeMillis())
    when (showConsole) {
      ShowConsole.ALWAYS -> {
        taskDescriptor.isActivateToolWindowWhenAdded = true
        taskDescriptor.isActivateToolWindowWhenFailed = true
      }

      ShowConsole.ON_FAIL -> {
        taskDescriptor.isActivateToolWindowWhenAdded = false
        taskDescriptor.isActivateToolWindowWhenFailed = true
      }

      ShowConsole.NEVER -> {
        taskDescriptor.isActivateToolWindowWhenAdded = false
        taskDescriptor.isActivateToolWindowWhenFailed = false
      }
    }
    addRedoActionToDescriptor(taskDescriptor, redoAction)
    addCancelActionToDescriptor(taskId, taskDescriptor, cancelAction)
    addPtyToDescriptor(taskId, taskDescriptor)
    val startEvent = StartBuildEvent.builder(message, taskDescriptor).build()
    taskView.onEvent(taskId, startEvent)
  }

  private fun addCancelActionToDescriptor(
    taskId: TaskId,
    taskDescriptor: DefaultBuildDescriptor,
    doCancelAction: () -> Unit,
  ) {
    val cancelAction = CancelAction(doCancelAction, taskId)
    taskDescriptor.withAction(cancelAction)
  }

  private fun addRedoActionToDescriptor(taskDescriptor: DefaultBuildDescriptor, redoAction: (suspend () -> Unit)? = null) {
    val action = calculateRedoAction(redoAction)
    taskDescriptor.withAction(action)
  }

  private fun addPtyToDescriptor(taskId: TaskId, taskDescriptor: DefaultBuildDescriptor) {
    if (!BazelFeatureFlags.usePty) return
    val consoleDelegate = TerminalExecutionConsoleBuilder(project).build()
    val console = object : ConsoleView by consoleDelegate, BuildProgressListener {
      override fun onEvent(buildId: Any, event: BuildEvent) {
        if (event is OutputBuildEvent) {
          consoleDelegate.print(event.message, ConsoleViewContentType.getConsoleViewType(event.outputType))
        }
      }
    }
    taskPtyTerminalMap[taskId] = consoleDelegate

    val contentDescriptor =
      BuildContentDescriptor(console, null, consoleDelegate.component, null)
    contentDescriptor.apply {
      // copy values from taskDescriptor, where it had been set before already
      isActivateToolWindowWhenAdded = taskDescriptor.isActivateToolWindowWhenAdded
      isActivateToolWindowWhenFailed = taskDescriptor.isActivateToolWindowWhenFailed
    }
    Disposer.register(contentDescriptor, consoleDelegate)
    Disposer.register(taskView as Disposable, contentDescriptor)

    // TerminalExecutionConsoleBuilder expects a ProcessHandler, so we provide a stub because we handle process interaction in the server.
    // See https://github.com/bazelbuild/intellij/pull/7001/files
    val ttyConnector = object : TtyConnector, Disposable {
      init {
        Disposer.register(contentDescriptor, this)
      }

      private var connected: Boolean = true
      override fun dispose() {
        connected = false
      }

      override fun read(buf: CharArray?, offset: Int, length: Int): Int = 0
      override fun write(bytes: ByteArray?) = Unit
      override fun write(string: String?) = Unit
      override fun isConnected(): Boolean = connected
      override fun waitFor(): Int = 0
      override fun ready(): Boolean = true
      override fun getName(): String = ""
      override fun close() = Unit
      override fun resize(termSize: TermSize) = Unit
    }

    consoleDelegate.terminalWidget.createTerminalSession(ttyConnector)
    consoleDelegate.terminalWidget.start()
    taskDescriptor.withContentDescriptor { contentDescriptor }
  }

  protected abstract fun calculateRedoAction(redoAction: (suspend () -> Unit)?): AnAction

  override fun hasTasksInProgress(): Boolean = tasksInProgress.isNotEmpty()

  @Synchronized
  override fun finishTask(
    taskId: TaskId,
    message: String,
    result: EventResult,
  ) {
    if (tasksInProgress.contains(taskId)) {
      doFinishTask(taskId, message, result)
    }
  }

  private fun doFinishTask(
    taskId: TaskId,
    message: String,
    result: EventResult,
  ) {
    if (result is FailureResultImpl) {
      result.failures.forEach {
        log.warn(it.message, it.error)
        it.error?.let { error ->
          addMessage(taskId, getStackTraceAsString(error))
        }
      }
    }
    finishChildrenSubtasks(taskId, result)
    tasksInProgress.remove(taskId)
    taskFailures.remove(taskId)
    subtaskParentMap.entries.removeAll { findActiveRootTaskId(it.value) == taskId }
    taskPtyTerminalMap.remove(taskId)
    val event = FinishBuildEventImpl(taskId, null, System.currentTimeMillis(), message, result)
    taskView.onEvent(taskId, event)
  }

  @Synchronized
  override fun startSubtask(
    subtaskId: TaskId,
    message: String,
  ) {
    val rootTaskId = findActiveRootTaskId(subtaskId) ?: return
    val activeTaskId = findActiveTaskId(subtaskId) ?: return
    if (activeTaskId == subtaskId)
      throw IllegalStateException("Cannot restart task $subtaskId")
    doStartSubtask(rootTaskId, activeTaskId, subtaskId, message)
  }

  private fun doStartSubtask(
    rootTaskId: TaskId,
    parentTaskId: TaskId,
    subtaskId: TaskId,
    message: String,
  ) {
    subtaskMessageMap[subtaskId] = message
    subtaskParentMap[subtaskId] = parentTaskId
    val event = ProgressBuildEventImpl(subtaskId, parentTaskId, System.currentTimeMillis(), message, -1, -1, "")
    taskView.onEvent(rootTaskId, event)
  }

  @Synchronized
  override fun finishSubtask(
    subtaskId: TaskId,
    message: String?,
    result: EventResult,
  ) {
    val parentTaskId = subtaskParentMap[subtaskId] ?: return
    val rootTaskId = findActiveRootTaskId(parentTaskId) ?: return
    doFinishSubtask(rootTaskId, subtaskId, message, result)
  }

  private fun doFinishSubtask(
    rootTask: TaskId,
    subtaskId: TaskId,
    message: String?,
    result: EventResult,
  ) {
    val savedMessage = subtaskMessageMap.remove(subtaskId) ?: ""
    val messageToDisplay = message ?: savedMessage

    finishChildrenSubtasks(subtaskId, result)
    val event = FinishEventImpl(subtaskId, null, System.currentTimeMillis(), messageToDisplay, result)
    taskView.onEvent(rootTask, event)

    subtaskParentMap.remove(subtaskId)
  }

  private fun finishChildrenSubtasks(parentId: TaskId, result: EventResult) {
    subtaskParentMap
      .filterValues { it == parentId }
      .keys
      .forEach {
        finishSubtask(it, null, result)
      }
  }

  @Synchronized
  override fun addDiagnosticMessage(
    taskId: TaskId,
    path: Path?,
    line: Int,
    column: Int,
    message: String,
    severity: MessageEvent.Kind,
  ) {
    if (message.isBlank()) return
    val rootTaskId = findActiveRootTaskId(taskId) ?: return
    val activeTaskId = findActiveTaskId(taskId) ?: return
    val filePosition = path?.let { FilePosition(path.toFile(), line, column) }
    doAddDiagnosticMessage(rootTaskId, activeTaskId, filePosition, message, severity)
  }

  private fun doAddDiagnosticMessage(
    taskId: TaskId,
    subtaskId: TaskId,
    filePosition: FilePosition?,
    message: String,
    severity: MessageEvent.Kind,
  ) {
    val event =
      if (filePosition?.file == null) {
        MessageEventImpl(
          subtaskId,
          severity,
          null,
          prepareTextToPrint(message),
          prepareTextToPrint(message),
        )
      }
      else {
        FileMessageEventImpl(
          subtaskId,
          severity,
          null,
          prepareTextToPrint(message),
          null,
          filePosition,
        )
      }

    taskView.onEvent(taskId, event)
  }

  @Synchronized
  override fun addMessage(taskId: TaskId, message: String) {
    if (message.isBlank()) return
    val rootTaskId = findActiveRootTaskId(taskId) ?: return
    val activeTaskId = findActiveTaskId(taskId) ?: return
    doAddMessage(activeTaskId, message)
  }

  @Synchronized
  override fun registerException(taskId: TaskId, ex: Throwable): Boolean {
    val rootTaskId = findActiveRootTaskId(taskId) ?: return true
    return taskFailures.getOrPut(rootTaskId) { mutableSetOf() }.add(ex)
  }

  private val DEFAULT_PTY_TERM_SIZE = TermSize(80, 24)

  @Synchronized
  override fun ptyTermSize(taskId: TaskId): TermSize? {
    val terminal = generateSequence(taskId) { it.parent }
      .firstNotNullOfOrNull { taskPtyTerminalMap[it] } ?: return null
    return terminal.terminalWidget.terminalPanel.terminalSizeFromComponent ?: DEFAULT_PTY_TERM_SIZE
  }

  private fun doAddMessage(taskId: TaskId, message: String) {
    if (tasksInProgress.contains(taskId)) {
      sendMessageEvent(taskId, message)
    }
    else if (subtaskParentMap.containsKey(taskId)) {
      sendMessageEvent(taskId, message)
      sendMessageToAncestors(taskId, message)
    }
  }

  private fun sendMessageToAncestors(taskId: TaskId, message: String): Unit {
    val parentTask = subtaskParentMap[taskId]
    if (parentTask != null) {
      sendMessageEvent(parentTask, message)
      sendMessageToAncestors(parentTask, message)
    }
  }

  private fun sendMessageEvent(taskId: TaskId, message: String) {
    val subtaskId = if (tasksInProgress.contains(taskId)) null else taskId
    val event = OutputBuildEventImpl(subtaskId, message, true)
    findActiveRootTaskId(taskId)?.let { rootTaskId ->
      taskView.onEvent(rootTaskId, event)
    }
  }

  private inline fun doIfTaskInProgress(taskId: TaskId, action: () -> Unit) {
    if (tasksInProgress.contains(taskId)) {
      action()
    }
  }

  private inline fun doUnlessTaskInProgress(taskId: TaskId, action: () -> Unit) {
    if (!tasksInProgress.contains(taskId)) {
      action()
    }
  }

  private fun findActiveTaskId(taskId: TaskId): TaskId? {
    var id: TaskId? = taskId
    while (id != null) {
      if (subtaskParentMap.containsKey(id) || tasksInProgress.contains(id))
        return id
      id = id.parent
    }
    return null
  }

  private fun findParentTaskId(taskId: TaskId): TaskId? {
    var id: TaskId? = taskId.parent
    while (id != null) {
      if (subtaskParentMap.containsKey(id) || tasksInProgress.contains(id))
        return id
      id = id.parent
    }
    return null
  }

  private fun findActiveRootTaskId(taskId: TaskId): TaskId? {
    var id: TaskId? = taskId
    while (id != null) {
      if (tasksInProgress.contains(id))
        return id
      id = id.parent
    }
    return null
  }

  private fun prepareTextToPrint(text: String): String = if (text.endsWith("\n")) text else text + "\n"

  private inner class CancelAction(private val doCancelAction: () -> Unit, private val taskId: TaskId) :
    DumbAwareAction({ BazelPluginBundle.message("cancel.action.text") }, AllIcons.Actions.Suspend) {
    @Volatile
    private var cancelActionActivated = false

    init {
      project.messageBus.connect().subscribe(
        SyncStatusListener.TOPIC,
        object : SyncStatusListener {
          override fun syncStarted() {}

          override fun syncFinished(canceled: Boolean) {}
        },
      )
    }

    override fun actionPerformed(e: AnActionEvent) {
      cancelActionActivated = true
      doCancelAction()
    }

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = !cancelActionActivated && tasksInProgress.contains(taskId)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
  }
}

internal class SyncTaskConsole(
  taskView: BuildProgressListener,
  basePath: String,
  project: Project,
) : BaseTaskConsole(taskView, basePath, project) {
  override fun calculateRedoAction(redoAction: (suspend () -> Unit)?): AnAction =
    object : SuspendableAction({ BazelPluginBundle.message("resync.action.text") }, AllIcons.Actions.Refresh) {
      @Volatile
      private var redoActionActivated = false

      override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
        redoActionActivated = true
        redoAction?.invoke()
      }

      override fun update(project: Project, e: AnActionEvent) {
        e.presentation.isEnabled = !redoActionActivated && redoAction != null && !project.isSyncInProgress() && !project.isBuildInProgress()
      }
    }
}

internal class BuildTaskConsole(
  taskView: BuildProgressListener,
  basePath: String,
  project: Project,
) : BaseTaskConsole(taskView, basePath, project) {
  override fun calculateRedoAction(redoAction: (suspend () -> Unit)?): AnAction =
    object : SuspendableAction({ BazelPluginBundle.message("rebuild.action.text") }, AllIcons.Actions.Compile) {
      @Volatile
      private var redoActionActivated = false

      override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
        redoActionActivated = true
        redoAction?.invoke()
      }

      override fun update(project: Project, e: AnActionEvent) {
        e.presentation.isEnabled = !redoActionActivated && redoAction != null && tasksInProgress.isEmpty()
      }

      override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
    }
}

internal fun Project.isBuildInProgress() = ConsoleService.getInstance(this).buildConsole.hasTasksInProgress()
