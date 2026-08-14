package org.jetbrains.bazel.workspace.fileEvents

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsSafe
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.impl.ProgressText
import com.intellij.platform.util.progress.impl.ScopedLambda
import com.intellij.platform.util.progress.reportSequentialProgress
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginBundle

@ApiStatus.Internal
class BazelFileEventProgressReporter(private val originalReporter: SequentialProgressReporter) {
  private enum class Step(val size: Int) {
    QUERY(40),
    MODIFY(40),
    FINALIZE(20),
  }

  suspend fun <T> queryStep(action: suspend CoroutineScope.() -> T): T {
    @Suppress("DialogTitleCapitalization")
    return step(
      step = Step.QUERY,
      text = BazelPluginBundle.message("file.change.processing.step.query"),
      action = action
    )
  }

  suspend fun <T> updateModelStep(action: suspend CoroutineScope.() -> T): T {
    return step(
      step = Step.MODIFY,
      text = BazelPluginBundle.message("file.change.processing.step.update"),
      action = action
    )
  }

  suspend fun <T> finalisingStep(action: suspend CoroutineScope.() -> T): T {
    return step(
      step = Step.FINALIZE,
      text = BazelPluginBundle.message("file.change.processing.step.commit"),
      action = action
    )
  }

  fun message(@NlsSafe message: String) {
    originalReporter.sizedStep(0, text = message)
  }

  private suspend fun <T> step(step: Step, text: ProgressText, action: suspend CoroutineScope.() -> T): T {
    return originalReporter.sizedStep(
      workSize = 0,
      text = text,
      action = action
    ).also {
      // move progress
      originalReporter.sizedStep(step.size)
      originalReporter.sizedStep(0)
    }
  }

  companion object {
    suspend fun runWithProgressBar(
      project: Project,
      actions: suspend (BazelFileEventProgressReporter) -> Unit,
    ) {
      withBackgroundProgress(project, BazelPluginBundle.message("file.change.processing.title")) {
        reportSequentialProgress(size = Step.entries.sumOf { it.size }) { originalReporter ->
          actions(BazelFileEventProgressReporter(originalReporter))
        }
      }
    }
  }
}
