package org.jetbrains.bazel.run.handler

import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo

class GenericTestHandlerProvider : GooglePluginAwareRunHandlerProvider {
  override val id: String = "GenericTestHandlerProvider"

  override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = GenericBazelTestHandler()

  override fun canRun(targetInfos: List<BuildTargetInfo>): Boolean = targetInfos.all { it.capabilities.canTest }

  override fun canDebug(targetInfos: List<BuildTargetInfo>): Boolean = false

  override val googleHandlerId: String = "BlazeCommandGenericRunConfigurationHandlerProvider"
  override val isTestHandler: Boolean = true
}
