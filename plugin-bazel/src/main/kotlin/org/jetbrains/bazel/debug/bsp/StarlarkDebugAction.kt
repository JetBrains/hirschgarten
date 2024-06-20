package org.jetbrains.bazel.debug.bsp

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
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.ui.actions.SuspendableAction

class StarlarkDebugAction(
  private val targetId: BuildTargetId,
) : SuspendableAction (
  text = BazelPluginBundle.message("starlark.debug.action.name"),
  icon = AllIcons.Actions.StartDebugger,
) {
  override suspend fun actionPerformed(project: Project, e: AnActionEvent) {
    val config = createConfigSettings(project)
    config.save(project)
    config.execute()
  }

  private fun createConfigSettings(project: Project): RunnerAndConfigurationSettings {
    val runManager = RunManager.getInstance(project)
    val configName =
      runManager.suggestUniqueName(
        BazelPluginBundle.message("starlark.debug.config.template", targetId),
        type = null,
      )
    val factory = StarlarkDebugConfigurationType().configurationFactories.first()
    return runManager.createConfiguration(configName, factory).withTarget(targetId)
  }

  private fun RunnerAndConfigurationSettings.withTarget(target: BuildTargetId): RunnerAndConfigurationSettings {
    (this.configuration as? StarlarkDebugConfiguration)?.setTarget(target)
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

  companion object {
    fun isApplicableTo(targetInfo: BuildTargetInfo): Boolean {
      return targetInfo.capabilities.canCompile
    }
  }
}
