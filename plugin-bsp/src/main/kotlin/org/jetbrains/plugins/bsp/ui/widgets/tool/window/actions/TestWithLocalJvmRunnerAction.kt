package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import ch.epfl.scala.bsp4j.JvmEnvironmentItem
import com.intellij.execution.Executor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import org.jetbrains.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.magicmetamodel.impl.workspacemodel.toBsp4JTargetIdentifier
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.server.tasks.JvmTestEnvironmentTask

internal class TestWithLocalJvmRunnerAction(
  targetInfo: BuildTargetInfo,
  text: () -> String = { BspPluginBundle.message("widget.test.target.with.runner.popup.message") },
) : LocalJvmRunnerAction(
  targetInfo = targetInfo,
  text = text,
  icon = AllIcons.Actions.Execute
) {
  override fun getEnvironment(project: Project): JvmEnvironmentItem? =
    JvmTestEnvironmentTask(project).connectAndExecute(targetInfo.id.toBsp4JTargetIdentifier())?.items?.first()

  override fun getExecutor(): Executor = DefaultRunExecutor.getRunExecutorInstance()
}
