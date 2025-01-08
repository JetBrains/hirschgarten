package org.jetbrains.plugins.bsp.run.handler

import org.jetbrains.plugins.bsp.run.BspRunHandler
import org.jetbrains.plugins.bsp.run.BspRunHandlerProvider
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo

class GenericBspRunHandlerProvider : BspRunHandlerProvider {
  override val id: String = "GenericBspRunHandlerProvider"

  override fun createRunHandler(configuration: BspRunConfiguration): BspRunHandler = GenericBspRunHandler()

  override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean = targetInfos.singleOrNull()?.capabilities?.canRun ?: false

  override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = false
}
