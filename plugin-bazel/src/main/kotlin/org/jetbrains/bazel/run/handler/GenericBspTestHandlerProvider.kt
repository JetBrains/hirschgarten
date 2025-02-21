package org.jetbrains.bazel.run.handler

import org.jetbrains.bazel.run.BspRunHandler
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BspRunConfiguration
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo

public class GenericBspTestHandlerProvider : RunHandlerProvider {
  override val id: String = "GenericBspTestHandlerProvider"

  override fun createRunHandler(configuration: BspRunConfiguration): BspRunHandler = GenericBspTestHandler()

  override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean = targetInfos.all { it.capabilities.canTest }

  override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = false
}
