package org.jetbrains.plugins.bsp.server.client

import ch.epfl.scala.bsp4j.BuildClient
import ch.epfl.scala.bsp4j.CompileReport
import ch.epfl.scala.bsp4j.CompileTask
import ch.epfl.scala.bsp4j.DiagnosticSeverity
import ch.epfl.scala.bsp4j.DidChangeBuildTarget
import ch.epfl.scala.bsp4j.LogMessageParams
import ch.epfl.scala.bsp4j.PrintParams
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams
import ch.epfl.scala.bsp4j.ShowMessageParams
import ch.epfl.scala.bsp4j.TaskFinishDataKind
import ch.epfl.scala.bsp4j.TaskFinishParams
import ch.epfl.scala.bsp4j.TaskProgressParams
import ch.epfl.scala.bsp4j.TaskStartDataKind
import ch.epfl.scala.bsp4j.TaskStartParams
import ch.epfl.scala.bsp4j.TestFinish
import ch.epfl.scala.bsp4j.TestReport
import ch.epfl.scala.bsp4j.TestStart
import ch.epfl.scala.bsp4j.TestTask
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.build.events.MessageEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.server.connection.TimeoutHandler
import org.jetbrains.plugins.bsp.services.BspTaskEventsService
import org.jetbrains.plugins.bsp.ui.console.TaskConsole

public const val importSubtaskId: String = "import-subtask-id"

public class BspClient(
  private val bspSyncConsole: TaskConsole,
  private val bspBuildConsole: TaskConsole,
  private val timeoutHandler: TimeoutHandler,
  private val project: Project,
) : BuildClient {
  private val log = logger<BspClient>()
  private val gson = Gson()

  private val bspLogger = bspLogger<BspClient>()

  override fun onBuildShowMessage(params: ShowMessageParams) {
    onBuildEvent()

    log.warn("Got show message: $params")

    val originId = params.originId ?: return // TODO
    val message = params.message ?: return // TODO

    BspTaskEventsService.getInstance(project).withListener(originId) {
      onShowMessage(message)
    }
  }

  override fun onBuildLogMessage(params: LogMessageParams) {
    onBuildEvent()

    log.warn("Got log message: $params")

    // Legacy task handling
    if (params.originId == null || !BspTaskEventsService.getInstance(project).existsListener(params.originId)) {
      addMessageToConsole(params.originId, params.message)
      return
    }

    val originId = params.originId ?: return // TODO
    val message = params.message ?: return // TODO

    BspTaskEventsService.getInstance(project).withListener(originId) {
      onLogMessage(message)
    }
  }

  override fun onBuildTaskStart(params: TaskStartParams) {
    onBuildEvent()

    val taskId = params.taskId.id

    log.warn("Got task start: $params")
    val originId = params.originId ?: return // TODO
    val maybeParent = params.taskId.parents?.firstOrNull()

    val data: Any? = when (params.dataKind) {
      TaskStartDataKind.TEST_START -> {
        gson.fromJson(params.data as JsonObject, TestStart::class.java)
      }

      TaskStartDataKind.TEST_TASK -> {
        gson.fromJson(params.data as JsonObject, TestTask::class.java)
      }

      TaskStartDataKind.COMPILE_TASK -> {
        gson.fromJson(params.data as JsonObject, CompileTask::class.java)
      }

      else -> null
    }

    BspTaskEventsService.getInstance(project).withListener(originId) {
      onTaskStart(taskId, maybeParent, params.message ?: taskId, data)
    }
  }

  override fun onBuildTaskProgress(params: TaskProgressParams) {
    onBuildEvent()

    log.warn("Got task progress: $params")

    val taskId = params.taskId.id
    val originId = params.originId ?: return // TODO

    BspTaskEventsService.getInstance(project).withListener(originId) {
      onTaskProgress(taskId, params.message, null)
    }
  }

  override fun onBuildTaskFinish(params: TaskFinishParams) {
    onBuildEvent()
    val taskId = params.taskId.id
    log.warn("Got task finish: $params")
    val originId = params.originId ?: return // TODO

    val data: Any? = when (params.dataKind) {
      TaskFinishDataKind.TEST_FINISH -> {
        gson.fromJson(params.data as JsonObject, TestFinish::class.java)
      }

      TaskFinishDataKind.TEST_REPORT -> {
        gson.fromJson(params.data as JsonObject, TestReport::class.java)
      }

      TaskFinishDataKind.COMPILE_REPORT -> {
        gson.fromJson(params.data as JsonObject, CompileReport::class.java)
      }

      else -> null
    }

    val status = params.status

    BspTaskEventsService.getInstance(project).withListener(originId) {
      onTaskFinish(taskId, params.message.orEmpty(), status, data)
    }
  }

  override fun onRunPrintStdout(params: PrintParams) {
    onBuildEvent()
    log.warn("Got print stdout: $params")
    val originId = params.originId ?: return // TODO
    val taskId = params.task.id
    val message = params.message ?: return // TODO

    BspTaskEventsService.getInstance(project).withListener(originId) {
      onOutputStream(taskId, message)
    }
  }

  override fun onRunPrintStderr(params: PrintParams) {
    onBuildEvent()
    log.warn("Got print stderr: $params")
    val originId = params.originId ?: return // TODO
    val taskId = params.task.id
    val message = params.message ?: return // TODO

    BspTaskEventsService.getInstance(project).withListener(originId) {
      onErrorStream(taskId, message)
    }
  }

  override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams) {
    onBuildEvent()

    // Legacy task handling
    if (params.originId == null || !BspTaskEventsService.getInstance(project).existsListener(params.originId)) {
      log.warn("Got diagnostics without listener: $params")
      addDiagnosticToConsole(params)
      return
    }

    val originId = params.originId ?: return // TODO
    val textDocument = params.textDocument.uri ?: return // TODO
    val buildTarget = params.buildTarget.uri ?: return // TODO

    BspTaskEventsService.getInstance(project).withListener(originId) {
      params.diagnostics.forEach {
        onDiagnostic(
          textDocument,
          buildTarget,
          it.range.start.line,
          it.range.start.character,
          getMessageEventKind(it.severity),
          it.message
        )
      }
    }
  }

  override fun onBuildTargetDidChange(params: DidChangeBuildTarget?) {
    onBuildEvent()
  }

  private fun onBuildEvent() {
    timeoutHandler.resetTimer()
  }

  private fun addMessageToConsole(originId: String?, message: String) {
    if (originId?.startsWith("build") == true || originId?.startsWith("mobile-install") == true) {
      bspBuildConsole.addMessage(originId, message)
    } else {
      bspSyncConsole.addMessage(originId ?: importSubtaskId, message)
    }
    bspLogger.info(message)
  }

  private fun addDiagnosticToConsole(params: PublishDiagnosticsParams) {
    if (params.originId != null && params.textDocument != null) {
      val targetConsole = if (params.originId?.startsWith("build") == true) bspBuildConsole else bspSyncConsole
      params.diagnostics.forEach {
        targetConsole.addDiagnosticMessage(
          params.originId,
          params.textDocument.uri,
          it.range.start.line,
          it.range.start.character,
          it.message,
          getMessageEventKind(it.severity),
        )
        logDiagnosticBySeverity(it.severity, it.message)
      }
    }
  }

  private fun getMessageEventKind(severity: DiagnosticSeverity?): MessageEvent.Kind = when (severity) {
    DiagnosticSeverity.ERROR -> MessageEvent.Kind.ERROR
    DiagnosticSeverity.WARNING -> MessageEvent.Kind.WARNING
    DiagnosticSeverity.INFORMATION -> MessageEvent.Kind.INFO
    DiagnosticSeverity.HINT -> MessageEvent.Kind.INFO
    null -> MessageEvent.Kind.SIMPLE
  }

  private fun logDiagnosticBySeverity(severity: DiagnosticSeverity?, message: String) {
    when (severity) {
      DiagnosticSeverity.ERROR -> bspLogger.error(message)
      DiagnosticSeverity.WARNING -> bspLogger.warn(message)
      DiagnosticSeverity.INFORMATION -> bspLogger.info(message)
      else -> bspLogger.trace(message)
    }
  }
}
