package org.jetbrains.bazel.taskEvents

import com.intellij.build.events.MessageEvent.Kind
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.logger
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.fus.BazelBuildMetricsCollector
import org.jetbrains.bsp.protocol.BazelInvocationMetrics
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler
import org.jetbrains.bsp.protocol.CachedTestLog
import org.jetbrains.bsp.protocol.CoverageReport
import org.jetbrains.bsp.protocol.DiagnosticSeverity
import org.jetbrains.bsp.protocol.LogMessageParams
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.PublishDiagnosticsParams
import org.jetbrains.bsp.protocol.TaskFinishParams
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.TaskStartParams
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
@ApiStatus.Internal
class BazelTaskEventsService(private val project: Project) : BazelTaskEventsHandler {

  private val taskListeners: ConcurrentHashMap<TaskGroupId, BazelTaskListener> = ConcurrentHashMap()

  fun saveListener(id: TaskGroupId, listener: BazelTaskListener) {
    if (id == TaskGroupId.EMPTY) {
      error("Attempt to register TaskGroupId.EMPTY listener")
    }
    if (taskListeners.putIfAbsent(id, listener) != null) {
      throw IllegalStateException("Listener for task $id exists already")
    }
  }

  fun withListener(id: TaskId, block: BazelTaskListener.() -> Unit) {
    // `TaskGroupId.EMPTY` is explicitly created to silence given steps
    if (id.taskGroupId == TaskGroupId.EMPTY) {
      return
    }

    val listener = taskListeners[id.taskGroupId] ?: run {
      log.warn("No task listener found for task $id")
      return
    }

    block(listener)
  }

  fun removeListener(id: TaskGroupId) {
    taskListeners.remove(id)
  }

  override fun onBuildTaskStart(params: TaskStartParams) {
    val taskId = params.taskId
    val message = params.message ?: return
    withListener(taskId) {
      onTaskStart(taskId, message, params.data)
    }
  }

  override fun onBuildTaskFinish(params: TaskFinishParams) {
    val taskId = params.taskId
    val status = params.status
    val message = params.message ?: return

    withListener(taskId) {
      onTaskFinish(taskId, message, status, params.data)
    }
  }

  override fun onBuildLogMessage(params: LogMessageParams) {
    val originId = params.task
    val message = params.message
    // Drop the empty-target-set warning emitted by the 0-target "probe" build the sync runs to read options.
    if (isEmptyTargetSetWarning(message)) return

    withListener(originId) {
      onLogMessage(originId, message)
    }
  }

  override fun onBuildPublishDiagnostics(params: PublishDiagnosticsParams) {
    withListener(params.taskId) {
      params.diagnostics.forEach { diag ->
        onDiagnostic(
          params.taskId,
          params.textDocument?.path,
          params.buildTarget,
          diag.range.start.line,
          diag.range.start.character,
          when (diag.severity) {
            DiagnosticSeverity.ERROR -> Kind.ERROR
            DiagnosticSeverity.WARNING -> Kind.WARNING
            DiagnosticSeverity.INFORMATION -> Kind.INFO
            DiagnosticSeverity.HINT -> Kind.INFO
            null -> Kind.SIMPLE
          },
          diag.message,
        )
      }
    }
  }

  override fun onPublishCoverageReport(report: CoverageReport) {
    withListener(report.taskId) {
      onPublishCoverageReport(report.coverageReport)
    }
  }

  override fun onCachedTestLog(testLog: CachedTestLog) {
    withListener(testLog.taskId) {
      onCachedTestLog(testLog.testLog)
    }
  }

  override fun onBazelInvocationMetrics(metrics: BazelInvocationMetrics) {
    BazelBuildMetricsCollector.log(project, metrics)
  }

  companion object {
    private val log = logger<BazelTaskEventsService>()

    @JvmStatic
    fun getInstance(project: Project) = project.service<BazelTaskEventsService>()
  }
}

/**
 * Recognizes Bazel's warning for an invocation with an empty target set, emitted line by line.
 *
 * The sync runs a 0-target "probe" build (to read the resolved options before the aspect build), which always
 * triggers this warning. A real user build has targets and won't produce it, so dropping it globally is safe.
 * Matched on stable, non-localized fragments observed on Bazel 9; see https://github.com/bazelbuild/bazel/issues/6811.
 */
@ApiStatus.Internal
fun isEmptyTargetSetWarning(line: String): Boolean {
  val lowercased = line.lowercase()
  return "usage: bazel build" in lowercased ||
    "invoke `bazel help build`" in lowercased ||
    "empty set of targets" in lowercased
}
