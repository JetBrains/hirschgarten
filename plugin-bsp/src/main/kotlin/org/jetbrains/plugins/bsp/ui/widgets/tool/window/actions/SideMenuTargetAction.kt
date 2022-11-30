package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerEx
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import org.jetbrains.plugins.bsp.runConfig.BspRunConfiguration

public val targetIdTOREMOVE: Key<BuildTargetIdentifier> = Key<BuildTargetIdentifier>("targetId")

internal abstract class SideMenuTargetAction(
  private val target: BuildTargetIdentifier,
  text: String,
) : AnAction(text) {

  abstract fun getConfigurationType(): ConfigurationType

  abstract fun getName(target: BuildTargetIdentifier): String

  override fun actionPerformed(e: AnActionEvent) {
    e.project?.let { project ->
      val factory = getConfigurationType().configurationFactories.first()
      val setting = RunManager.getInstance(project).createConfiguration(getName(target), factory).also {
        val c = it.configuration as BspRunConfiguration
        c.target = target.uri
        // TODO: remove this
      }
      RunManagerEx.getInstanceEx(project).setTemporaryConfiguration(setting)
      val runExecutor = DefaultRunExecutor.getRunExecutorInstance()
      ProgramRunner.getRunner(runExecutor.id, setting.configuration)?.let {
        try {
          val executionEnvironment = ExecutionEnvironmentBuilder(project, runExecutor)
            .runnerAndSettings(it, setting)
            .build()
          it.execute(executionEnvironment)
        } catch (e: Exception) {
          Messages.showErrorDialog(project, e.message, "Error")
        }
      }
    }
  }
}
