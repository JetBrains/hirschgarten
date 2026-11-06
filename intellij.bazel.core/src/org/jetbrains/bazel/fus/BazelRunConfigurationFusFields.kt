package org.jetbrains.bazel.fus

import com.intellij.execution.impl.statistics.RunConfigurationTypeDefs
import com.intellij.execution.impl.statistics.RunConfigurationTypeUsagesCollector
import com.intellij.internal.statistic.eventLog.events.EventField
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.eventLog.events.EventPair
import com.intellij.internal.statistic.eventLog.validator.rules.impl.ExtensionIdValidationRule
import com.intellij.internal.statistic.service.fus.collectors.FeatureUsageCollectorExtension
import org.jetbrains.bazel.run.RunHandlerProvider

/**
 * The FUS fields that [org.jetbrains.bazel.run.config.BazelRunConfiguration] adds to the platform run configuration events.
 */
internal object BazelRunConfigurationFusFields {
  private val HANDLER_PROVIDER = EventFields.StringValidatedByCustomRule(
    "bazel_handler_provider",
    BazelRunHandlerProviderValidationRule::class.java,
    description = "The id of the run handler provider of the Bazel run configuration.",
  )

  val fields: List<EventField<*>> = listOf(HANDLER_PROVIDER)

  fun usageData(handlerProvider: RunHandlerProvider?): List<EventPair<*>> =
    handlerProvider?.let { listOf(HANDLER_PROVIDER.with(it.id)) } ?: emptyList()
}

/**
 * Accepts the id of a registered [RunHandlerProvider], and marks the id of a provider from a third-party plugin.
 */
internal class BazelRunHandlerProviderValidationRule : ExtensionIdValidationRule<RunHandlerProvider>(RunHandlerProvider.ep, { it.id })

/**
 * Registers [BazelRunConfigurationFusFields] for the event of a started run configuration.
 */
internal class BazelRunConfigurationUsageCollectorExtension : FeatureUsageCollectorExtension {
  override fun getGroupId(): String = RunConfigurationTypeDefs.TRIGGER_USAGES_GROUP_ID

  override fun getEventId(): String = "started"

  override fun getExtensionFields(): List<EventField<*>> = BazelRunConfigurationFusFields.fields
}

/**
 * Registers [BazelRunConfigurationFusFields] for the state event of a run configuration in the project.
 */
internal class BazelRunConfigurationTypeCollectorExtension : FeatureUsageCollectorExtension {
  override fun getGroupId(): String = RunConfigurationTypeDefs.TYPE_USAGES_GROUP_ID

  override fun getEventId(): String = RunConfigurationTypeUsagesCollector.CONFIGURED_IN_PROJECT

  override fun getExtensionFields(): List<EventField<*>> = BazelRunConfigurationFusFields.fields
}
