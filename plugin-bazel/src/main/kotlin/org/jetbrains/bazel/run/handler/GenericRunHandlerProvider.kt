package org.jetbrains.bazel.run.handler

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bsp.protocol.BuildTarget

class GenericRunHandlerProvider : GooglePluginAwareRunHandlerProvider {
  override val id: String = "GenericRunHandlerProvider"

  override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = GenericBazelRunHandler()

  override fun canRun(targetInfos: List<BuildTarget>): Boolean = targetInfos.singleOrNull()?.kind?.ruleType == RuleType.BINARY

  override fun canDebug(targetInfos: List<BuildTarget>): Boolean = false

  override val googleHandlerId: String = "BlazeCommandGenericRunConfigurationHandlerProvider"
  override val isTestHandler: Boolean = false
}
