/*
 * Copyright 2024 The Bazel Authors. All rights reserved.
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
package org.jetbrains.bazel.ogRun.producers

import com.google.common.collect.ImmutableList
import com.google.idea.blaze.base.command.BlazeCommandName
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.bazel.ogRun.producers.BlazeBuildTargetRunConfigurationProducer.Companion.getBuildTarget

/** Creates run configurations from a BUILD file targets.
 * Based on BlazeBuildFileRunConfigurationProducer.java
 */
class BlazeBuildTargetRunConfigurationProducer

    : BlazeRunConfigurationProducer<BlazeCommandRunConfiguration?>(BlazeCommandRunConfigurationType.getInstance()) {
    override fun doSetupConfigFromContext(
        configuration: BlazeCommandRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement?>
    ): Boolean {
        val project: Project = configuration.getProject()
        val blazeProjectData: BlazeProjectData? =
            BlazeProjectDataManager.getInstance(project).getBlazeProjectData()
        // With query sync we don't need a sync to run a configuration
        if (blazeProjectData == null && Blaze.getProjectType(project) !== ProjectType.QUERY_SYNC) {
            return false
        }
        val target: BuildTarget? = Companion.getBuildTarget(context)
        if (target == null) {
            return false
        }
        sourceElement.set(target.rule)
        setupConfiguration(configuration.getProject(), blazeProjectData, configuration, target)
        return true
    }

    override fun doIsConfigFromContext(
        configuration: BlazeCommandRunConfiguration, context: ConfigurationContext
    ): Boolean {
        val target: BuildTarget? = Companion.getBuildTarget(context)
        if (target == null) {
            return false
        }
        return configuration.targets == ImmutableList.of<Any?>(target.label)
    }

    companion object {
        private fun getBuildTarget(context: ConfigurationContext): BuildTarget? {
            return getBuildTarget(
                PsiTreeUtil.getNonStrictParentOfType(context.getPsiLocation(), FuncallExpression::class.java)
            )
        }

        fun getBuildTarget(rule: FuncallExpression?): BuildTarget? {
            if (rule == null) {
                return null
            }
            val ruleType: String? = rule.getFunctionName()
            val label: Label? = rule.resolveBuildLabel()
            if (ruleType == null || label == null) {
                return null
            }
            return BuildTarget(rule, Kind.guessRuleType(ruleType), label)
        }

        private fun setupConfiguration(
            ignoredProject: Project?,
            ignoredBlazeProjectData: BlazeProjectData?,
            configuration: BlazeCommandRunConfiguration,
            target: BuildTarget
        ) {
            val info: TargetInfo? = target.guessTargetInfo()
            if (info != null) {
                configuration.setTargetInfo(info)
            } else {
                configuration.setTarget(target.label)
            }
            val state: BlazeCommandRunConfigurationCommonState? =
                configuration.getHandlerStateIfType<BlazeCommandRunConfigurationCommonState?>(
                    BlazeCommandRunConfigurationCommonState::class.java
                )
            if (state != null) {
                state.commandState.setCommand(BlazeCommandName.BUILD)
            }
            configuration.setGeneratedName()
        }
    }
}
