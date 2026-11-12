package org.jetbrains.bazel.run.handler

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.Nls
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run.BazelRunHandler
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.import.GooglePluginAwareRunHandlerProvider
import org.jetbrains.bsp.protocol.BuildTargetInfo

internal class GenericTestHandlerProvider : GooglePluginAwareRunHandlerProvider {
  override val id: String
    get() = "GenericTestHandlerProvider"

  override val providedRunHandlerName: @Nls String
    get() = BazelPluginBundle.message("generic.test.handler.name")

  override fun createRunHandler(configuration: BazelRunConfiguration): BazelRunHandler = GenericBazelTestHandler()

  override fun canRun(project: Project, targets: List<BuildTargetInfo>): Boolean = targets.all { it.kind.ruleType == RuleType.TEST }

  override fun canRunNonImported(project: Project, targets: List<Label>): Boolean = true

  override val googleHandlerId: String = "BlazeCommandGenericRunConfigurationHandlerProvider"
  override val isTestHandler: Boolean = true
}
