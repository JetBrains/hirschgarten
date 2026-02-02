package org.jetbrains.bazel.server.client

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bazel.ui.console.TaskConsole
import org.jetbrains.bsp.protocol.CachedTestLog
import org.jetbrains.bsp.protocol.CoverageReport
import org.jetbrains.bsp.protocol.DiagnosticSeverity
import org.jetbrains.bsp.protocol.JoinedBuildClient
import org.jetbrains.bsp.protocol.LogMessageParams
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.TaskFinishParams
import org.jetbrains.bsp.protocol.TaskStartParams

const val IMPORT_SUBTASK_ID: String = "import-subtask-id"

class BazelClient(
  private val syncConsole: TaskConsole,
  private val buildConsole: TaskConsole,
  private val project: Project,
) : JoinedBuildClient {
  private val log = logger<BazelClient>()

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
    log.debug("Got task start: $params")
    BazelTaskEventsService.getInstance(project).onBuildTaskStart(params)
  }

  override fun onBuildTaskFinish(params: TaskFinishParams) {
    log.debug("Got task finish: $params")
    BazelTaskEventsService.getInstance(project).onBuildTaskFinish(params)
  }

  override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams) {
    if (!BazelTaskEventsService.getInstance(project).existsListener(params.originId)) {
      log.debug("Got diagnostics without listener: $params")
      addDiagnosticToConsole(params)
      return
    }

    val originId = params.originId
    val textDocument = params.textDocument?.path
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

  private fun addMessageToConsole(originId: String?, message: String) {
    if (originId?.startsWith("build") == true || originId?.startsWith("mobile-install") == true) {
      buildConsole.addMessage(originId, message)
    } else {
      syncConsole.addMessage(originId, message)
    }
  }

  private fun addDiagnosticToConsole(params: PublishDiagnosticsParams) {
    val targetConsole = if (params.originId.startsWith("build")) buildConsole else syncConsole
    params.diagnostics.forEach {
      targetConsole.addDiagnosticMessage(
        params.originId,
        params.textDocument?.path,
        it.range.start.line,
        it.range.start.character,
        it.message,
        getMessageEventKind(it.severity),
      )
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

  override fun onPublishCoverageReport(report: CoverageReport) {
    BazelTaskEventsService.getInstance(project).withListener(report.originId) {
      onPublishCoverageReport(report.coverageReport)
    }
  }

  override fun onCachedTestLog(testLog: CachedTestLog) {
    BazelTaskEventsService.getInstance(project).withListener(testLog.originId) {
      onCachedTestLog(testLog.testLog)
    }
  }
}
