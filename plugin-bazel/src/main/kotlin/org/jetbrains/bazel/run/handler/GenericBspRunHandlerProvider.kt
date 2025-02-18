package org.jetbrains.bazel.run.handler

import org.jetbrains.bazel.run.BspRunHandler
import org.jetbrains.bazel.run.BspRunHandlerProvider
import org.jetbrains.bazel.run.config.BspRunConfiguration
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo

class GenericBspRunHandlerProvider : BspRunHandlerProvider {
  override val id: String = "GenericBspRunHandlerProvider"

  override fun createRunHandler(configuration: BspRunConfiguration): BspRunHandler = GenericBspRunHandler()

  override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean = targetInfos.singleOrNull()?.capabilities?.canRun ?: false

  override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = false
}
