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
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import com.intellij.platform.diagnostic.telemetry.EDT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.ui.actions.SuspendableAction
import javax.swing.Icon

public val targetIdTOREMOVE: Key<BuildTargetId> = Key<BuildTargetId>("targetId")

internal abstract class SideMenuRunnerAction(
  protected val targetId: BuildTargetId,
  text: () -> String,
  icon: Icon? = null,
) : SuspendableAction(text, icon) {
  abstract fun getConfigurationType(): ConfigurationType

  abstract fun getName(target: BuildTargetId): String

  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    withContext(Dispatchers.EDT) {
      doPerformAction(project, targetId)
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
      executionEnvironment.putUserData(targetIdTOREMOVE, targetId)
      runner.execute(executionEnvironment)
    } catch (e: Exception) {
      Messages.showErrorDialog(project, e.message, "Error")
    }
  }
}
