package org.jetbrains.plugins.bsp.intellij

import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.run.BspRunHandler
import org.jetbrains.plugins.bsp.run.BspRunHandlerProvider
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration

private const val INTELLIJ_PLUGIN_TAG = "intellij-plugin"

class IntellijPluginRunHandlerProvider : BspRunHandlerProvider {
  override val id: String = "IntellijPluginRunHandlerProvider"

  override fun createRunHandler(configuration: BspRunConfiguration): BspRunHandler = IntellijPluginRunHandler(configuration)

  override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean =
    targetInfos.singleOrNull()?.tags?.contains(INTELLIJ_PLUGIN_TAG)
      ?: false

  override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = true
}
