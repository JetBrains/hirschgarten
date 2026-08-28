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
import org.jetbrains.bazel.progress.ConsoleService
import org.jetbrains.bazel.progress.TaskConsole
import org.jetbrains.bazel.ui.console.task.TestTaskConsole

internal class TestConsoleService(project: Project) : ConsoleService, Disposable {
  override val buildConsole: TaskConsole
  override val syncConsole: TaskConsole

  private val log = logger<TestConsoleService>()

  private fun String?.trimCrLf(): String? =
    this?.trimEnd { it.isWhitespace() || it == '\r' || it == '\n' }

  private fun onEventImpl(buildId: Any, event: BuildEvent) {
    if (event is FinishEvent && event.result is FailureResult) {
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
    if (event is MessageEvent) {
      when (event.kind) {
        MessageEvent.Kind.ERROR -> log.warn("Bazel build error: ${event.message.trimCrLf()}")
        MessageEvent.Kind.WARNING -> log.warn("Bazel build warning: ${event.message.trimCrLf()}")
        else -> log.warn("Bazel build message: ${event.message.trimCrLf()}")
      }
    }
    if (event is OutputBuildEvent && event.parentId == null) {
      log.info("Bazel build message: ${event.message.trimCrLf()}")
    }
  }

  override fun dispose() {}

  init {
    buildConsole = TestTaskConsole(
      object : BuildViewManager(project) {
        override fun onEvent(buildId: Any, event: BuildEvent) {
          onEventImpl(buildId, event)
          super.onEvent(buildId, event)
        }
      }.also { Disposer.register(this, it) },
      "", project,
    )

    syncConsole = TestTaskConsole(
      object : SyncViewManager(project) {
        override fun onEvent(buildId: Any, event: BuildEvent) {
          onEventImpl(buildId, event)
          super.onEvent(buildId, event)
        }
      }.also { Disposer.register(this, it) },
      "", project,
    )
  }
}
