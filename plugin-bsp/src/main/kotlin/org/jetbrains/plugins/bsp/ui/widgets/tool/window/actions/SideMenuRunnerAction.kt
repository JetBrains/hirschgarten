package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.execution.Executor
import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerEx
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.ui.actions.SuspendableAction
import org.jetbrains.plugins.bsp.ui.configuration.run.BspRunConfiguration
import org.jetbrains.plugins.bsp.ui.configuration.test.BspTestRunConfiguration
import javax.swing.Icon

internal abstract class SideMenuRunnerAction(
  protected val targetId: BuildTargetId,
  text: () -> String,
  icon: Icon? = null,
  private val useDebugExecutor: Boolean = false,
) : SuspendableAction(text, icon) {
  abstract fun getConfigurationType(project: Project): ConfigurationType

  abstract fun getName(target: BuildTargetId): String

  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    withContext(Dispatchers.EDT) {
      doPerformAction(project, targetId)
    }
  }

  fun doPerformAction(project: Project, targetId: BuildTargetId) {
    val factory = getConfigurationType(project).configurationFactories.first()
    val settings = RunManager.getInstance(project).createConfiguration(getName(targetId), factory)
    prepareRunConfiguration(settings.configuration)
    RunManagerEx.getInstanceEx(project).setTemporaryConfiguration(settings)
    val executor = getExecutorInstance()
    ProgramRunner.getRunner(executor.id, settings.configuration)?.let {
      executor.executeWithRunner(it, settings, project)
    }
  }

  /** This function is called after a run configuration instance is created
   * (it is passed as this function's argument), but before it's executed. */
  protected open fun prepareRunConfiguration(configuration: RunConfiguration) {
    // nothing by default
  }

  private fun getExecutorInstance(): Executor =
    if (useDebugExecutor) {
      DefaultDebugExecutor.getDebugExecutorInstance()
    } else {
      DefaultRunExecutor.getRunExecutorInstance()
    }

  private fun Executor.executeWithRunner(
    runner: ProgramRunner<RunnerSettings>,
    settings: RunnerAndConfigurationSettings,
    project: Project,
  ) {
    try {
      val executionEnvironment = ExecutionEnvironmentBuilder(project, this)
        .runnerAndSettings(runner, settings)
        .build()
      when (val config = settings.configuration) {
        is BspRunConfiguration -> config.targetUri = targetId
        is BspTestRunConfiguration -> config.targetUri = targetId
      }
      runner.execute(executionEnvironment)
    } catch (e: Exception) {
      Messages.showErrorDialog(project, e.message, BspPluginBundle.message("widget.side.menu.error.title"))
    }
  }
}
