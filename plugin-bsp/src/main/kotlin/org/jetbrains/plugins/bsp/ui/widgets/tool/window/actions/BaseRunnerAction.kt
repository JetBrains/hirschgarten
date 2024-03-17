package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.execution.Executor
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
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.actions.SuspendableAction
import javax.swing.Icon

internal abstract class BaseRunnerAction(
  private val buildTargetInfo: BuildTargetInfo,
  text: () -> String,
  icon: Icon? = null,
  private val isDebugAction: Boolean = false,
) : SuspendableAction(
  text = text,
  icon = icon ?: if (isDebugAction) AllIcons.Actions.StartDebugger else AllIcons.Actions.Execute
) {
  abstract suspend fun getRunnerSettings(
    project: Project,
    buildTargetInfo: BuildTargetInfo,
  ): RunnerAndConfigurationSettings?

  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    doPerformAction(project)
  }

  suspend fun doPerformAction(project: Project) {
    try {
      val settings = getRunnerSettings(project, buildTargetInfo) ?: return
      RunManagerEx.getInstanceEx(project).setTemporaryConfiguration(settings)
      val executor = getExecutor()
      ProgramRunner.getRunner(executor.id, settings.configuration)?.let { runner ->
        val executionEnvironment = ExecutionEnvironmentBuilder(project, executor)
          .runnerAndSettings(runner, settings)
          .build()
        withContext(Dispatchers.EDT) { runner.execute(executionEnvironment) }
      }
    } catch (e: Exception) {
      withContext(Dispatchers.EDT) {
        Messages.showErrorDialog(project, e.message, BspPluginBundle.message("widget.side.menu.error.title"))
      }
    }
  }

  private fun getExecutor(): Executor =
    if (isDebugAction) {
      DefaultDebugExecutor.getDebugExecutorInstance()
    } else {
      DefaultRunExecutor.getRunExecutorInstance()
    }
}
