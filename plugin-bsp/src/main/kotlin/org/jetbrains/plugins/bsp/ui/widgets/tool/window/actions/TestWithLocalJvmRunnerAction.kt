package org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions

import ch.epfl.scala.bsp4j.JvmEnvironmentItem
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.BspPluginBundle
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.toBsp4JTargetIdentifier
import org.jetbrains.plugins.bsp.server.tasks.JvmTestEnvironmentTask
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.getBuildTargetName

internal class TestWithLocalJvmRunnerAction(
  targetInfo: BuildTargetInfo,
  text: (() -> String)? = null,
  isDebugMode: Boolean = false,
  verboseText: Boolean = false,
) : LocalJvmRunnerAction(
  targetInfo = targetInfo,
  text = {
    if (text != null) text()
    else if (isDebugMode) BspPluginBundle.messageVerbose(
      "target.debug.with.jvm.runner.action.text",
      verboseText,
      targetInfo.getBuildTargetName()
    )
    else BspPluginBundle.messageVerbose(
      "target.test.with.jvm.runner.action.text",
      verboseText,
      targetInfo.getBuildTargetName()
    )
  },
  isDebugMode = isDebugMode,
) {
  override fun getEnvironment(project: Project): JvmEnvironmentItem? =
    JvmTestEnvironmentTask(project).connectAndExecute(targetInfo.id.toBsp4JTargetIdentifier())?.items?.first()
}
