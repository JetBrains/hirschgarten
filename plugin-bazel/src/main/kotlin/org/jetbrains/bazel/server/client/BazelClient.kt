package org.jetbrains.bazel.server.client

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bazel.ui.console.TaskConsole
import org.jetbrains.bazel.ui.console.ids.PROJECT_SYNC_TASK_ID
import org.jetbrains.bsp.protocol.CoverageReport
import org.jetbrains.bsp.protocol.DiagnosticSeverity
import org.jetbrains.bsp.protocol.DidChangeBuildTarget
import org.jetbrains.bsp.protocol.JoinedBuildClient
import org.jetbrains.bsp.protocol.LogMessageParams
import org.jetbrains.bsp.protocol.PrintParams
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.ShowMessageParams
import org.jetbrains.bsp.protocol.TaskFinishParams
import org.jetbrains.bsp.protocol.TaskProgressParams
import org.jetbrains.bsp.protocol.TaskStartParams

const val IMPORT_SUBTASK_ID: String = "import-subtask-id"

class BazelClient(
  private val syncConsole: TaskConsole,
  private val buildConsole: TaskConsole,
  private val project: Project,
) : JoinedBuildClient {
  private val log = logger<BazelClient>()

  private val bazelLogger = bazelLogger<BazelClient>()

  override fun onBuildShowMessage(params: ShowMessageParams) {
    log.debug("Got show message: $params")

    val originId = params.originId
    val message = params.message

    BazelTaskEventsService.getInstance(project).withListener(originId) {
      onShowMessage(message)
    }
  }

  override fun onBuildLogMessage(params: LogMessageParams) {
    log.debug("Got log message: $params")

    // Legacy task handling
    if (params.originId == null || !BazelTaskEventsService.getInstance(project).existsListener(params.originId!!)) {
      addMessageToConsole(params.originId, params.message)
      return
    }

    val originId = params.originId ?: return // TODO
    val message = params.message

    BazelTaskEventsService.getInstance(project).withListener(originId) {
      onLogMessage(message)
    }
  }

  override fun onBuildTaskStart(params: TaskStartParams) {
    val taskId = params.taskId.id

    log.debug("Got task start: $params")
    val originId = params.originId
    val maybeParent = params.taskId.parents?.firstOrNull()

    BazelTaskEventsService.getInstance(project).withListener(originId) {
      onTaskStart(taskId, maybeParent, params.message.orEmpty(), params.data)
    }
  }

  override fun onBuildTaskProgress(params: TaskProgressParams) {
    log.debug("Got task progress: $params")

    val taskId = params.taskId.id
    val originId = params.originId

    BazelTaskEventsService.getInstance(project).withListener(originId) {
      onTaskProgress(taskId, params.message.orEmpty(), null)
    }
  }

  override fun onBuildTaskFinish(params: TaskFinishParams) {
    val taskId = params.taskId.id
    log.debug("Got task finish: $params")
    val originId = params.originId
    val maybeParent = params.taskId.parents?.firstOrNull()

    val status = params.status

    BazelTaskEventsService.getInstance(project).withListener(originId) {
      onTaskFinish(taskId, maybeParent, params.message.orEmpty(), status, params.data)
    }
  }

  override fun onRunPrintStdout(params: PrintParams) {
    log.debug("Got print stdout: $params")
    val originId = params.originId
    val taskId = params.task.id
    val message = params.message

    BazelTaskEventsService.getInstance(project).withListener(originId) {
      onOutputStream(taskId, message)
    }
  }

  override fun onRunPrintStderr(params: PrintParams) {
    log.debug("Got print stderr: $params")
    val originId = params.originId
    val taskId = params.task.id
    val message = params.message

    BazelTaskEventsService.getInstance(project).withListener(originId) {
      onErrorStream(taskId, message)
    }
  }

  override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams) {
    if (!BazelTaskEventsService.getInstance(project).existsListener(params.originId)) {
      log.debug("Got diagnostics without listener: $params")
      addDiagnosticToConsole(params)
      return
    }

    val originId = params.originId
    val textDocument = params.textDocument.uri
    val buildTarget = params.buildTarget

    BazelTaskEventsService.getInstance(project).withListener(originId) {
      params.diagnostics.forEach {
        onDiagnostic(
          textDocument,
          buildTarget,
          it.range.start.line,
          it.range.start.character,
          getMessageEventKind(it.severity),
          it.message,
        )
      }
    }
  }

  override fun onBuildTargetDidChange(params: DidChangeBuildTarget) {
  }

  private fun addMessageToConsole(originId: String?, message: String) {
    if (originId?.startsWith("build") == true || originId?.startsWith("mobile-install") == true) {
      buildConsole.addMessage(originId, message)
    } else {
      syncConsole.addMessage(originId, message)
    }
    bazelLogger.info(message)
  }

  private fun addDiagnosticToConsole(params: PublishDiagnosticsParams) {
    val targetConsole = if (params.originId.startsWith("build")) buildConsole else syncConsole
    params.diagnostics.forEach {
      targetConsole.addDiagnosticMessage(
        params.originId ?: PROJECT_SYNC_TASK_ID,
        params.textDocument.uri,
        it.range.start.line,
        it.range.start.character,
        it.message,
        getMessageEventKind(it.severity),
      )
      logDiagnosticBySeverity(it.severity, it.message)
    }
  }

  private fun getMessageEventKind(severity: DiagnosticSeverity?): MessageEvent.Kind =
    when (severity) {
      DiagnosticSeverity.ERROR -> MessageEvent.Kind.ERROR
      DiagnosticSeverity.WARNING -> MessageEvent.Kind.WARNING
      DiagnosticSeverity.INFORMATION -> MessageEvent.Kind.INFO
      DiagnosticSeverity.HINT -> MessageEvent.Kind.INFO
      null -> MessageEvent.Kind.SIMPLE
    }

  private fun logDiagnosticBySeverity(severity: DiagnosticSeverity?, message: String) {
    when (severity) {
      DiagnosticSeverity.ERROR -> bazelLogger.error(message)
      DiagnosticSeverity.WARNING -> bazelLogger.warn(message)
      DiagnosticSeverity.INFORMATION -> bazelLogger.info(message)
      else -> bazelLogger.trace(message)
    }
  }

  override fun onPublishCoverageReport(report: CoverageReport) {
    BazelTaskEventsService.getInstance(project).withListener(report.originId) {
      onPublishCoverageReport(report.coverageReport)
    }
  }
}
