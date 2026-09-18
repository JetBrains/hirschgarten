package org.jetbrains.bazel.test.framework

import com.intellij.build.BuildViewManager
import com.intellij.build.SyncViewManager
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.FailureResult
import com.intellij.build.events.FinishEvent
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.OutputBuildEvent
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.bazel.config.BazelBackendBundle
import org.jetbrains.bazel.progress.ConsoleService
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bazel.ui.console.task.TestTaskConsole
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Fails unless the last sync of [project] finished with the `Sync done.` message.
 *
 * A sync that ends with `Sync partially succeeded with errors` does not log an error, so a test
 * that expects a clean sync must call this. The failure message lists the sync warnings and errors.
 */
internal fun assertLastSyncSucceeded(project: Project) {
  val console = ConsoleService.getInstance(project) as TestConsoleService
  val lastFinishMessage = console.syncFinishMessages.lastOrNull()
  val diagnostics = console.syncDiagnostics.joinToString("\n")
  assertEquals(BazelBackendBundle.message("console.task.sync.success"), lastFinishMessage) {
    "The last sync did not succeed. Sync diagnostics:\n$diagnostics"
  }
}

internal class TestConsoleService(project: Project) : ConsoleService, Disposable {
  override val buildConsole: TaskConsole
  override val syncConsole: TaskConsole

  /** The finish message of every sync task, oldest first. */
  val syncFinishMessages: List<String>
    field = ArrayList<String>()

  /** Every warning and error message of the sync console, oldest first. */
  val syncDiagnostics: List<String>
    field = ArrayList<String>()

  private val log = logger<TestConsoleService>()

  private fun String?.trimCrLf(): String? =
    this?.trimEnd { it.isWhitespace() || it == '\r' || it == '\n' }

  private fun storeEvent(event: BuildEvent) {
    when (event) {
        is FinishEvent ->
          syncFinishMessages.add(event.message.trimCrLf().orEmpty())

      is MessageEvent ->
        if (event.kind == MessageEvent.Kind.WARNING || event.kind == MessageEvent.Kind.ERROR) {
          syncDiagnostics.add(event.message.trimCrLf().orEmpty())
        }
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
            failure.failures.firstOrNull()?.error,
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
    buildConsole = TestTaskConsole(
      object : BuildViewManager(project) {
        override fun onEvent(buildId: Any, event: BuildEvent) {
          storeEvent(event)
          logEvent(event)
          super.onEvent(buildId, event)
        }
      }.also { Disposer.register(this, it) },
      "", project,
    )

    syncConsole = TestTaskConsole(
      object : SyncViewManager(project) {
        override fun onEvent(buildId: Any, event: BuildEvent) {
          storeEvent(event)
          logEvent(event)
          super.onEvent(buildId, event)
        }
      }.also { Disposer.register(this, it) },
      "", project,
    )
  }
}
