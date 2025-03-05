package org.jetbrains.bazel.run.handler

import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo

class GenericRunHandlerProvider : RunHandlerProvider {
  override val id: String = "GenericRunHandlerProvider"

  override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = GenericBazelRunHandler()

  override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean = targetInfos.singleOrNull()?.capabilities?.canRun ?: false

  override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = false
}
