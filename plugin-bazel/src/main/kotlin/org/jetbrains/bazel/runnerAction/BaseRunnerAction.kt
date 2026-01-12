package org.jetbrains.bazel.runnerAction

import com.intellij.coverage.CoverageExecutor
import com.intellij.execution.Executor
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.RunManagerEx
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.action.SuspendableAction
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bsp.protocol.BuildTarget
import javax.swing.Icon

public abstract class BaseRunnerAction(
  text: () -> String,
  icon: Icon? = null,
  private val isDebugAction: Boolean = false,
  private val isCoverageAction: Boolean = false,
) : SuspendableAction(
    text = text,
    icon = icon ?: getIcon(isDebugAction, isCoverageAction),
  ) {
  protected abstract suspend fun getRunnerSettings(project: Project, buildTargets: List<BuildTarget>): RunnerAndConfigurationSettings?

  protected abstract fun getBuildTargets(project: Project): List<BuildTarget>

  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    doPerformAction(project)
  }

  suspend fun doPerformAction(project: Project) {
    try {
      val settings = getRunnerSettings(project, this.getBuildTargets(project)) ?: return
      RunManagerEx.getInstanceEx(project).setTemporaryConfiguration(settings)
      val executor = getExecutor()
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

  private fun getExecutor(): Executor {
    require(!isDebugAction || !isCoverageAction) { "Coverage with debug not supported" }
    return if (isDebugAction) {
      DefaultDebugExecutor.getDebugExecutorInstance()
    } else if (isCoverageAction) {
      checkNotNull(ExecutorRegistry.getInstance().getExecutorById(CoverageExecutor.EXECUTOR_ID)) { "Can't get Coverage executor" }
    } else {
      DefaultRunExecutor.getRunExecutorInstance()
    }
  }

  companion object {
    private fun getIcon(isDebugAction: Boolean, isCoverageAction: Boolean): Icon =
      when {
        isCoverageAction -> AllIcons.General.RunWithCoverage
        isDebugAction -> AllIcons.Actions.StartDebugger
        else -> AllIcons.Actions.Execute
      }
  }
}
