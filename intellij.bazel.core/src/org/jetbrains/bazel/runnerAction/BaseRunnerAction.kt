package org.jetbrains.bazel.runnerAction

import com.intellij.execution.Executor
import com.intellij.execution.RunManagerEx
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BazelPluginBundle

@ApiStatus.Internal
abstract class BaseRunnerAction(
  private val executor: Executor,
  protected val configurationName: String,
) : SuspendableAction(
  text = executor.getStartActionText(configurationName),
  icon = executor.icon,
) {
  protected abstract suspend fun getRunnerSettings(): RunnerAndConfigurationSettings?

  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    doPerformAction(project)
  }

  suspend fun doPerformAction(project: Project) {
    try {
      val settings = getRunnerSettings() ?: return
      RunManagerEx.getInstanceEx(project).setTemporaryConfiguration(settings)
      val runner = ProgramRunner.getRunner(executor.id, settings.configuration)

      if (runner != null) {
        val executionEnvironment =
          ExecutionEnvironmentBuilder(project, executor)
            .runnerAndSettings(runner, settings)
            .build()
        withContext(Dispatchers.EDT) { runner.execute(executionEnvironment) }
      } else {
        error("Runner not found for executor ${executor.id}")
      }
    } catch (e: Exception) {
      withContext(Dispatchers.EDT) {
        Messages.showErrorDialog(project, e.toString(), BazelPluginBundle.message("widget.side.menu.error.title"))
      }
    }
  }
}
