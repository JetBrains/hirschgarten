package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import ch.epfl.scala.bsp4j.JvmEnvironmentItem
import com.intellij.execution.Executor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.toBsp4JTargetIdentifier
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.tasks.JvmRunEnvironmentTask

internal class RunWithLocalJvmRunnerAction(targetInfo: BuildTargetInfo): LocalJvmRunnerAction(
  targetInfo = targetInfo,
  text = { BspPluginBundle.message("widget.run.target.with.runner.popup.message") },
  icon = AllIcons.Actions.Execute
) {
  override fun getEnvironment(project: Project): JvmEnvironmentItem? =
    JvmRunEnvironmentTask(project).connectAndExecute(targetInfo.id.toBsp4JTargetIdentifier())?.items?.first()

  override fun getExecutor(): Executor = DefaultRunExecutor.getRunExecutorInstance()
}
