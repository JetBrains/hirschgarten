package org.jetbrains.bazel.run.handler

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bsp.protocol.BuildTarget

class GenericTestHandlerProvider : GooglePluginAwareRunHandlerProvider {
  override val id: String = "GenericTestHandlerProvider"

  override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = GenericBazelTestHandler()

  override fun canRun(targetInfos: List<BuildTarget>): Boolean = targetInfos.all { it.kind.ruleType == RuleType.TEST }

  override fun canDebug(targetInfos: List<BuildTarget>): Boolean = false

  override val googleHandlerId: String = "BlazeCommandGenericRunConfigurationHandlerProvider"
  override val isTestHandler: Boolean = true
}
