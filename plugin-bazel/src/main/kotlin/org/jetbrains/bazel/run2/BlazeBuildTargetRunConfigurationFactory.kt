/*
 * Copyright 2016 The Bazel Authors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.bazel.run2

import com.google.common.collect.ImmutableSet
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.command.BlazeCommandName
import org.jetbrains.bazel.info.BspTargetInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.run2.state.BlazeCommandRunConfigurationCommonState
import org.jetbrains.bazel.run2.targetfinder.TargetFinder

/**
 * A factory creating run configurations based on BUILD file targets. Runs last, as a fallback for
 * the case where no more specialized factory handles the target.
 */
class BlazeBuildTargetRunConfigurationFactory : BlazeRunConfigurationFactory() {
  public override fun handlesTarget(project: Project, projectData: BlazeProjectData, label: Label): Boolean {
    return findProjectTarget(project, label) != null
  }

  override val configurationFactory: ConfigurationFactory
    get() = BlazeCommandRunConfigurationType.getInstance().factory

  public override fun setupConfiguration(configuration: RunConfiguration, label: Label) {
    val blazeConfig = configuration as BlazeCommandRunConfiguration
    val target: BspTargetInfo.TargetInfo? = findProjectTarget(configuration.project, label)
    if (target == null) {
      return
    }
    blazeConfig.setTargetInfo(target)


    val state: BlazeCommandRunConfigurationCommonState? =
      blazeConfig.getHandlerStateIfType<BlazeCommandRunConfigurationCommonState>(
        BlazeCommandRunConfigurationCommonState::class.java
      )
    if (state != null) {
      state.commandState.command = commandForRuleType(target.getRuleType())
    }
    blazeConfig.setGeneratedName()
  }

  companion object {
    // The rule types we auto-create run configurations for during sync.
    private val HANDLED_RULE_TYPES: ImmutableSet<RuleType?> = ImmutableSet.of<RuleType?>(RuleType.TEST, RuleType.BINARY)

    private fun findProjectTarget(project: Project?, label: Label?): BspTargetInfo.TargetInfo? {
      val targetInfo = TargetFinder.findTargetInfo(project!!, label!!)
      if (targetInfo == null) {
        return null
      }
      return if (HANDLED_RULE_TYPES.contains(targetInfo.getRuleType())) targetInfo else null
    }

    private fun commandForRuleType(ruleType: RuleType): BlazeCommandName {
      return when (ruleType) {
        RuleType.BINARY -> BlazeCommandName.RUN
        RuleType.TEST -> BlazeCommandName.TEST
        else -> BlazeCommandName.BUILD
      }
    }
  }
}
