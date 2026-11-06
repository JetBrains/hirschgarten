package org.jetbrains.bazel.fus

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.runConfigurationType
import com.intellij.execution.impl.statistics.RunConfigurationTypeUsagesCollector
import com.intellij.internal.statistic.FUCollectorTestCase
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext
import com.intellij.internal.statistic.eventLog.validator.rules.impl.CustomValidationRule
import com.jetbrains.fus.reporting.api.ValidationResultType
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.RunHandlerProvider
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.config.BazelRunConfigurationType
import org.jetbrains.bazel.test.framework.BazelTestApplication
import org.jetbrains.bazel.test.framework.bazelCodeInsightFixture
import org.junit.jupiter.api.Test

@BazelTestApplication
class BazelRunConfigurationFusTest {
  private val myFixture by bazelCodeInsightFixture()

  @Test
  fun `reports the handler provider of a Bazel run configuration in the project`() {
    val project = myFixture.project
    val runManager = RunManager.getInstance(project)
    val settings = runManager.createConfiguration("app", runConfigurationType<BazelRunConfigurationType>().configurationFactories.first())
    val provider = RunHandlerProvider.findRunHandlerProvider(GENERIC_RUN_HANDLER_PROVIDER_ID)!!
    (settings.configuration as BazelRunConfiguration).updateRunProvider(listOf(Label.parse("//app:bin")), provider)
    runManager.addConfiguration(settings)

    val metrics = FUCollectorTestCase.collectProjectStateCollectorEvents(RunConfigurationTypeUsagesCollector::class.java, project)

    val bazelMetric = metrics.single { it.data.build()["id"] == BazelRunConfigurationType.ID }
    bazelMetric.data.build()["additional"] shouldBe mapOf("bazel_handler_provider" to GENERIC_RUN_HANDLER_PROVIDER_ID)
  }

  @Test
  fun `accepts only the ids of registered handler providers`() {
    val rule = CustomValidationRule.EP_NAME.extensionList.single { it.ruleId == "extension.org.jetbrains.bazel.runHandlerProvider" }
    val context = EventContext.create("started", emptyMap())

    rule.validate(GENERIC_RUN_HANDLER_PROVIDER_ID, context) shouldBe ValidationResultType.ACCEPTED
    rule.validate("UnknownHandlerProvider", context) shouldBe ValidationResultType.REJECTED
  }
}

private const val GENERIC_RUN_HANDLER_PROVIDER_ID = "GenericRunHandlerProvider"
