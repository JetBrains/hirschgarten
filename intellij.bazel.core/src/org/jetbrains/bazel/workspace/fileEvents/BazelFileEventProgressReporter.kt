package org.jetbrains.bazel.workspace.fileEvents

import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.SequentialProgressReporter
import com.intellij.platform.util.progress.reportSequentialProgress
import org.jetbrains.bazel.config.BazelPluginBundle

internal class BazelFileEventProgressReporter(private val originalReporter: SequentialProgressReporter) {
  fun startPreparationStep() {
    originalReporter.nextStep(PROGRESS_PREPARATION)
  }

  @Suppress("DialogTitleCapitalization", "RedundantSuppression") // "Querying Bazel" has title capitalization, it's a false positive
  suspend fun <T> startQueryStep(queryAction: suspend () -> T): T {
    originalReporter.nextStep(
      endFraction = PROGRESS_QUERY,
      text = BazelPluginBundle.message("file.change.processing.step.query"),
    )
    val result = queryAction()
    originalReporter.nextStep(PROGRESS_ADDING_FILES)
    return result
  }

  fun skipQueryStep() {
    originalReporter.nextStep(PROGRESS_QUERY)
    // instantly push the progress bar forward
    originalReporter.nextStep(PROGRESS_ADDING_FILES)
  }

  fun startFinalisingStep() {
    originalReporter.nextStep(PROGRESS_END)
  }

  companion object {
    suspend fun runWithProgressBar(
      progressTitle: String,
      project: Project,
      actions: suspend (BazelFileEventProgressReporter) -> Unit,
    ) {
      withBackgroundProgress(project, progressTitle) {
        reportSequentialProgress(size = PROGRESS_END) { originalReporter ->
          actions(BazelFileEventProgressReporter(originalReporter))
        }
      }
    }
  }
}

private const val PROGRESS_PREPARATION = 20
private const val PROGRESS_QUERY = 50
private const val PROGRESS_ADDING_FILES = 80
private const val PROGRESS_END = 100
