package org.jetbrains.bazel.test.framework

import com.intellij.build.BuildProgressListener
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.FailureResult
import com.intellij.build.events.FinishEvent
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.OutputBuildEvent
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.isControlFlowException
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.bazel.progress.ConsoleService
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bazel.sync.ProjectSyncCompletionResult
import org.jetbrains.bazel.sync.ProjectSyncOutcome
import org.jetbrains.bazel.sync.status.SyncStatusService
import org.jetbrains.bazel.ui.console.task.TestTaskConsole

/**
 * Fails unless the last sync of [project] ended in [ProjectSyncCompletionResult.SUCCESS], and also when no sync finished.
 *
 * A sync that ends in a partial success does not log an error, so a test that expects a clean sync must call this. The
 * failure message lists the sync warnings and errors. A partial success fails here, unlike [ProjectSyncOutcome.isSuccessful].
 *
 * When [allowWarnings] is false, this also fails if the sync console of [project] has a warning or an error.
 */
fun assertLastSyncSucceeded(project: Project, allowWarnings: Boolean = true) {
  val outcome = SyncStatusService.getInstance(project).status.value.lastOutcome
  val succeeded = outcome?.completionResult == ProjectSyncCompletionResult.SUCCESS
  if (succeeded && allowWarnings) return

  val diagnostics = (ConsoleService.getInstance(project) as TestConsoleService).syncDiagnostics
  if (succeeded && diagnostics.isEmpty()) return

  val reason = if (succeeded) {
    "reported ${diagnostics.size} warning(s) or error(s)"
  }
  else {
    "did not succeed. Its result: ${outcome?.completionResult ?: "no sync finished"}"
  }
  throw AssertionError("The last sync $reason. Sync diagnostics:\n${diagnostics.joinToString("\n")}", outcome?.failureCause)
}

internal class TestConsoleService(project: Project) : ConsoleService, Disposable {
  override val buildConsole: TaskConsole
  override val syncConsole: TaskConsole

  /** Every warning and error that the sync console shows, as a message or as the error of a failed task, oldest first. */
  val syncDiagnostics: List<String>
    field = ArrayList<String>()

  private val log = logger<TestConsoleService>()

  private fun String?.trimCrLf(): String? =
    this?.trimEnd { it.isWhitespace() || it == '\r' || it == '\n' }

  private fun storeEvent(event: BuildEvent) {
    if (event is MessageEvent && (event.kind == MessageEvent.Kind.WARNING || event.kind == MessageEvent.Kind.ERROR)) {
      val description = event.description.trimCrLf()?.takeIf { it.isNotBlank() && it != event.message.trimCrLf() }
      syncDiagnostics.add(listOfNotNull(event.message.trimCrLf(), description).joinToString("\n"))
    }
    if (event is FinishEvent) {
      (event.result as? FailureResult)?.failures?.forEach { syncDiagnostics.add(it.description ?: it.message ?: it.error?.message.orEmpty()) }
    }
  }

  private fun logEvent(event: BuildEvent) {
    when (event) {
        is FinishEvent if event.result is FailureResult -> {
          val failure = event.result as FailureResult
          log.error(
            "Bazel build finished with error: ${event.message.trimCrLf()} " +
            failure.failures.joinToString(";") { f ->
              buildString {
                if (f.message != null) append(f.message.trimCrLf())
                if (f.description != null) append(" (").append(f.message.trimCrLf()).append(")")
                if (f.error != null) appendLine().append(f.error).appendLine()
              }
            },
            failure.failures.firstOrNull()?.error?.takeUnless { it.isControlFlowException },
          )
        }

      is MessageEvent -> {
        when (event.kind) {
          MessageEvent.Kind.ERROR -> log.warn("Bazel build error: ${event.message.trimCrLf()}")
          MessageEvent.Kind.WARNING -> log.warn("Bazel build warning: ${event.message.trimCrLf()}")
          else -> log.warn("Bazel build message: ${event.message.trimCrLf()}")
        }
      }

      is OutputBuildEvent if event.parentId == null -> {
        log.info("Bazel build message: ${event.message.trimCrLf()}")
      }
    }
  }

  override fun dispose() {}

  init {
    buildConsole = TestTaskConsole(recordingListener(), "", project)
    syncConsole = TestTaskConsole(recordingListener(), "", project)
  }

  /** Stores and logs each event. It shows no build view, so no work of a build view stays on the EDT after the test. */
  private fun recordingListener(): BuildProgressListener =
    object : BuildProgressListener, Disposable {
      override fun onEvent(buildId: Any, event: BuildEvent) {
        storeEvent(event)
        logEvent(event)
      }

      override fun dispose() {}
    }.also { Disposer.register(this, it) }
}
