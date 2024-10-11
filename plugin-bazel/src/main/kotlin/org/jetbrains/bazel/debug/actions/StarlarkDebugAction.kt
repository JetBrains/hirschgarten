package org.jetbrains.bazel.debug.actions

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.RunManagerEx
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.debug.configuration.StarlarkDebugConfiguration
import org.jetbrains.bazel.debug.configuration.StarlarkDebugConfigurationType
import org.jetbrains.plugins.bsp.action.SuspendableAction
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

class StarlarkDebugAction(private val targetId: BuildTargetIdentifier) :
  SuspendableAction(
    text = BazelPluginBundle.message("starlark.debug.action.name"),
    icon = AllIcons.Actions.StartDebugger,
  ) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val config = createConfigSettings(project)
    config.isTemporary = true
    config.save(project)
    config.execute()
  }

  private fun createConfigSettings(project: Project): RunnerAndConfigurationSettings {
    val runManager = RunManager.getInstance(project)
    val configName = BazelPluginBundle.message("starlark.debug.config.template", targetId.uri)
    val factory = StarlarkDebugConfigurationType().configurationFactories.first()
    return runManager.createConfiguration(configName, factory).withTarget(targetId)
  }

  companion object {
    fun isApplicableTo(targetInfo: BuildTargetInfo): Boolean = targetInfo.capabilities.canCompile
  }
}

private fun RunnerAndConfigurationSettings.withTarget(target: BuildTargetIdentifier): RunnerAndConfigurationSettings {
  (this.configuration as? StarlarkDebugConfiguration)?.setTarget(target.uri)
  return this
}

private fun RunnerAndConfigurationSettings.save(project: Project) {
  RunManagerEx.getInstanceEx(project).setTemporaryConfiguration(this)
}

private fun RunnerAndConfigurationSettings.execute() {
  val executor =
    ExecutorRegistry.getInstance().getExecutorById(DefaultDebugExecutor.EXECUTOR_ID)
      ?: error("Failed to obtain the executor")
  ProgramRunnerUtil.executeConfiguration(this, executor)
}
