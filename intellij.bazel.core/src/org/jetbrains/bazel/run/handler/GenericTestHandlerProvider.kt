package org.jetbrains.bazel.run.handler

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider

internal class GenericTestHandlerProvider : GooglePluginAwareRunHandlerProvider {
  override val id: String
    get() = "GenericTestHandlerProvider"

  override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = GenericBazelTestHandler()

  override fun canRun(targets: List<TargetKind>): Boolean = targets.all { it.ruleType == RuleType.TEST }

  override fun canRunNonImported(project: Project, targets: List<Label>): Boolean = true

  override val googleHandlerId: String = "BlazeCommandGenericRunConfigurationHandlerProvider"
  override val isTestHandler: Boolean = true
}
