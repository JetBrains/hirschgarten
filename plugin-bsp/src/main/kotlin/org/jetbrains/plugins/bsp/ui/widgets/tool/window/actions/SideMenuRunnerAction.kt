package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import com.intellij.execution.Executor
import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerEx
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId

public val targetIdTOREMOVE: Key<BuildTargetId> = Key<BuildTargetId>("targetId")

internal abstract class SideMenuRunnerAction(
  text: String,
) : AbstractActionWithTarget(text) {
  abstract fun getConfigurationType(): ConfigurationType

  abstract fun getName(target: BuildTargetId): String

  override fun actionPerformed(e: AnActionEvent) {
    e.project?.let { project ->
      target?.let { target ->
        doPerformAction(project, target)
      }
    }
  }

  fun doPerformAction(project: Project, targetId: BuildTargetId) {
    val factory = getConfigurationType().configurationFactories.first()
    val settings = RunManager.getInstance(project).createConfiguration(getName(targetId), factory)
    RunManagerEx.getInstanceEx(project).setTemporaryConfiguration(settings)
    val runExecutor = DefaultRunExecutor.getRunExecutorInstance()
    ProgramRunner.getRunner(runExecutor.id, settings.configuration)?.let {
      runExecutor.executeWithRunner(it, settings, project)
    }
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
      // TODO shouldnt we use 'target' for that?
      executionEnvironment.putUserData(targetIdTOREMOVE, target)
      runner.execute(executionEnvironment)
    } catch (e: Exception) {
      Messages.showErrorDialog(project, e.message, "Error")
    }
  }
}
