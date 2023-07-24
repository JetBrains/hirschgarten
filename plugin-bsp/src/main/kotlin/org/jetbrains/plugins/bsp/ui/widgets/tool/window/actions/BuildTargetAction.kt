package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.withBackgroundProgress
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellationException
import org.jetbrains.plugins.bsp.server.tasks.BuildTargetTask
import org.jetbrains.plugins.bsp.services.BspCoroutineService
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle
import java.util.concurrent.ExecutionException

public class BuildTargetAction : AbstractActionWithTarget(
        BspAllTargetsWidgetBundle.message("widget.build.target.popup.message")) {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project

    if (project != null) {
      doAction(project)
    } else {
      log.warn("BuildTargetAction cannot be performed! Project not available.")
    }
  }

  private fun doAction(project: Project) {
    BspCoroutineService.getInstance(project).start {
      try {
        withBackgroundProgress(project, "Building...") {
          target?.let {
            BuildTargetTask(project).connectAndExecute(it)
          }
        }
      } catch (e: Exception) {
        when {
          doesCompletableFutureGetThrowCancelledException(e) -> {}
          else -> log.error(e)
        }
      }
    }
  }

  private companion object {
    private val log = logger<BuildTargetAction>()
  }
}

public fun doesCompletableFutureGetThrowCancelledException(e: Exception): Boolean =
  (e is ExecutionException || e is InterruptedException) && e.cause is CancellationException
