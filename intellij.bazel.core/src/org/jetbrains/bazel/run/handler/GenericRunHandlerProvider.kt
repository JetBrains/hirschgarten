package org.jetbrains.bazel.run.handler

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.label.AllRuleTargets
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bsp.protocol.BuildTarget

internal class GenericRunHandlerProvider : GooglePluginAwareRunHandlerProvider {
  override val id: String
    get() = "GenericRunHandlerProvider"

  override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = GenericBazelRunHandler()

  override fun canRun(project: Project, targets: List<BuildTarget>): Boolean = targets.singleOrNull()?.kind?.ruleType == RuleType.BINARY

  override fun canRunNonImported(project: Project, targets: List<Label>): Boolean =
    targets.size == 1 && targets.single().target !is AllRuleTargets

  override val googleHandlerId: String = "BlazeCommandGenericRunConfigurationHandlerProvider"
  override val isTestHandler: Boolean = false
}
