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
package org.jetbrains.bazel.ogRun.producers

import com.google.common.collect.ImmutableList
import com.google.idea.blaze.base.command.BlazeCommandName
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.bazel.ogRun.producers.BlazeBuildFileRunConfigurationProducer.Companion.getBuildTarget
import java.util.*
import java.util.function.Consumer

/** Creates run configurations from a BUILD file targets.  */
class BlazeBuildFileRunConfigurationProducer

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
        return configuration.getHandler().getCommandName() != null
    }

    override fun doIsConfigFromContext(
        configuration: BlazeCommandRunConfiguration, context: ConfigurationContext
    ): Boolean {
        val target: BuildTarget? = Companion.getBuildTarget(context)
        if (target == null) {
            return false
        }
        if (configuration.targets != ImmutableList.of<Any?>(target.label)) {
            return false
        }

        // We don't know any details about how the various factories set up configurations from here.
        // Simply returning true at this point would be overly broad
        // (all configs with a matching target would be identified).
        // A complete equality check, meanwhile, would be too restrictive
        // (things like config name and user flags shouldn't count)
        // - not to mention we lack the equals() implementations needed to perform such a check!

        // So we compromise: if the target, suggested name, and command name match,
        // we consider it close enough. The suggested name is checked because it tends
        // to cover what the handler considers important,
        // and ignores changes the user may have made to the name.
        val blazeProjectData: BlazeProjectData? =
            BlazeProjectDataManager.getInstance(configuration.getProject()).getBlazeProjectData()
        if (blazeProjectData == null) {
            return false
        }
        val generatedConfiguration: BlazeCommandRunConfiguration =
            BlazeCommandRunConfiguration(
                configuration.getProject(), configuration.getFactory(), configuration.getName()
            )
        setupConfiguration(
            configuration.getProject(), blazeProjectData, generatedConfiguration, target
        )

        // ignore filtered test configs, produced by other configuration producers.
        val handlerState: BlazeCommandRunConfigurationCommonState? =
            configuration.getHandlerStateIfType<BlazeCommandRunConfigurationCommonState?>(
                BlazeCommandRunConfigurationCommonState::class.java
            )
        if (handlerState != null && handlerState.testFilterFlag != null) {
            return false
        }

        return configuration.suggestedName() == generatedConfiguration.suggestedName()
                && configuration.getHandler().getCommandName() == generatedConfiguration.getHandler().getCommandName()
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
            project: Project?,
            blazeProjectData: BlazeProjectData,
            configuration: BlazeCommandRunConfiguration,
            target: BuildTarget
        ) {
            // First see if a BlazeRunConfigurationFactory can give us a specialized setup.
            for (configurationFactory in BlazeRunConfigurationFactory.EP_NAME.extensions) {
                if (configurationFactory.handlesTarget(project, blazeProjectData, target.label)
                    && configurationFactory.handlesConfiguration(configuration)
                ) {
                    configurationFactory.setupConfiguration(configuration, target.label)
                    return
                }
            }

            // If no factory exists, directly set up the configuration.
            setupBuildFileConfiguration(configuration, target)
        }

        private fun setupBuildFileConfiguration(
            config: BlazeCommandRunConfiguration, target: BuildTarget
        ) {
            val info: TargetInfo? = target.guessTargetInfo()
            if (info != null) {
                config.setTargetInfo(info)
            } else {
                config.setTarget(target.label)
            }
            val state: BlazeCommandRunConfigurationCommonState? =
                config.getHandlerStateIfType<BlazeCommandRunConfigurationCommonState?>(
                    BlazeCommandRunConfigurationCommonState::class.java
                )
            if (state != null) {
                val blazeCommandName: Optional<BlazeCommandName?> = commandForRuleType(target.ruleType)
                blazeCommandName.ifPresent(Consumer { command: BlazeCommandName? ->
                    state.commandState.setCommand(
                        command
                    )
                }
                )
            }
            config.setGeneratedName()
        }

        fun commandForRuleType(ruleType: RuleType): Optional<BlazeCommandName?> {
            when (ruleType) {
                BINARY -> return Optional.of<BlazeCommandName?>(BlazeCommandName.RUN)
                TEST -> return Optional.of<BlazeCommandName?>(BlazeCommandName.TEST)
                else -> return Optional.empty<BlazeCommandName?>()
            }
        }
    }
}
