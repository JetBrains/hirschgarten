package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerEx
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import org.jetbrains.plugins.bsp.services.BspUtilService
import org.jetbrains.plugins.bsp.ui.test.configuration.BspConfigurationType
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.all.targets.BspAllTargetsWidgetBundle

public class TestTargetAction (
  private val target: BuildTargetIdentifier
) : AnAction(BspAllTargetsWidgetBundle.message("widget.test.target.popup.message")) {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project!!

    val factory  = BspConfigurationType().configurationFactories[0]
    val setting = RunManager.getInstance(project).createConfiguration(target.uri.toString().substringAfter(':'), factory)
    val configuration = setting.configuration
    val runManagerEx = RunManagerEx.getInstanceEx(project)
    runManagerEx.setTemporaryConfiguration(setting)
    val runExecutor = DefaultRunExecutor.getRunExecutorInstance()
    val runner = ProgramRunner.getRunner(runExecutor.id, configuration)
    if (runner != null) {
      try {
        val builder = ExecutionEnvironmentBuilder(project, runExecutor)
        builder.runnerAndSettings(runner, setting)
        val executionEnvironment = builder.build()
        executionEnvironment.putUserData(BspUtilService.targetIdKey, target)
        runner.execute(executionEnvironment)
      }
      catch (e: Exception) {
        Messages.showErrorDialog(project, e.message, "error")
      }
    }
  }
}