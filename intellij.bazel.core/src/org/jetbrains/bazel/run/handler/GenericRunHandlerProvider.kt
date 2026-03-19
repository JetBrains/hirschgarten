package org.jetbrains.bazel.run.handler

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider

internal class GenericRunHandlerProvider : GooglePluginAwareRunHandlerProvider {
  override val id: String
    get() = "GenericRunHandlerProvider"

  override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = GenericBazelRunHandler()

  override fun canRun(targets: List<TargetKind>): Boolean = targets.singleOrNull()?.ruleType == RuleType.BINARY

  override fun canDebug(targets: List<TargetKind>): Boolean = false

  override val googleHandlerId: String = "BlazeCommandGenericRunConfigurationHandlerProvider"
  override val isTestHandler: Boolean = false
}
