package org.jetbrains.plugins.bsp.run.handler

import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.run.BspRunHandler
import org.jetbrains.plugins.bsp.run.BspRunHandlerProvider
import org.jetbrains.plugins.bsp.run.config.BspRunConfiguration

public class GenericBspTestHandlerProvider : BspRunHandlerProvider {
  override val id: String = "GenericBspTestHandlerProvider"

  override fun createRunHandler(configuration: BspRunConfiguration): BspRunHandler = GenericBspTestHandler()

  override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean = targetInfos.all { it.capabilities.canTest }

  override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = false
}
