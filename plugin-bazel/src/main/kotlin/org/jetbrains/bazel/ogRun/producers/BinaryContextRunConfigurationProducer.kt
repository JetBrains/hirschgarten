/*
 * Copyright 2018 The Bazel Authors. All rights reserved.
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

import com.google.common.annotations.VisibleForTesting
import com.google.common.collect.ImmutableList
import com.google.idea.blaze.base.command.BlazeCommandName
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import java.util.*

/** Produces run configurations via [BinaryContextProvider].  */
class BinaryContextRunConfigurationProducer
internal constructor() :
    BlazeRunConfigurationProducer<BlazeCommandRunConfiguration?>(BlazeCommandRunConfigurationType.getInstance()) {
    /** Implements [.equals] so that cached value stability checker passes.  */
    private class ContextWrapper(val context: ConfigurationContext) {
        override fun equals(obj: Any?): Boolean {
            return obj is ContextWrapper
                    && context.getPsiLocation() == obj.context.getPsiLocation()
        }

        override fun hashCode(): Int {
            return Objects.hash(this.javaClass, context.getPsiLocation())
        }
    }

    private fun findRunContext(context: ConfigurationContext): BinaryRunContext? {
        if (!SmRunnerUtils.getSelectedSmRunnerTreeElements(context).isEmpty()) {
            // not a binary run context
            return null
        }
        val wrapper = ContextWrapper(context)
        val psi = context.getPsiLocation()
        return if (psi == null)
            null
        else
            CachedValuesManager.getCachedValue<T?>(
                psi,
                CachedValueProvider {
                    CachedValueProvider.Result.create<T?>(
                        doFindRunContext(wrapper.context),
                        PsiModificationTracker.MODIFICATION_COUNT,
                        BlazeSyncModificationTracker.getInstance(wrapper.context.getProject())
                    )
                })
    }

    private fun doFindRunContext(context: ConfigurationContext?): BinaryRunContext? {
        return Arrays.stream<BinaryContextProvider?>(BinaryContextProvider.EP_NAME.extensions)
            .map<BinaryRunContext?> { p: BinaryContextProvider? -> p!!.getRunContext(context) }
            .filter { obj: BinaryRunContext? -> Objects.nonNull(obj) }
            .findFirst()
            .orElse(null)
    }

    override fun doSetupConfigFromContext(
        configuration: BlazeCommandRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement?>
    ): Boolean {
        val runContext: BinaryRunContext? = findRunContext(context)
        if (runContext == null) {
            return false
        }
        sourceElement.set(runContext.sourceElement)
        configuration.setTargetInfo(runContext.target)
        val handlerState: BlazeCommandRunConfigurationCommonState? =
            configuration.getHandlerStateIfType<BlazeCommandRunConfigurationCommonState?>(
                BlazeCommandRunConfigurationCommonState::class.java
            )
        if (handlerState == null) {
            return false
        }
        handlerState.commandState.setCommand(BlazeCommandName.RUN)
        configuration.setGeneratedName()
        return true
    }

    @VisibleForTesting
    public override fun doIsConfigFromContext(
        configuration: BlazeCommandRunConfiguration, context: ConfigurationContext
    ): Boolean {
        val commonState: BlazeCommandRunConfigurationCommonState? =
            configuration.getHandlerStateIfType<BlazeCommandRunConfigurationCommonState?>(
                BlazeCommandRunConfigurationCommonState::class.java
            )
        if (commonState == null) {
            return false
        }
        if (commonState.commandState.getCommand() != BlazeCommandName.RUN) {
            return false
        }
        val runContext: BinaryRunContext? = findRunContext(context)
        if (runContext == null) {
            return false
        }
        val targets: ImmutableList<out TargetExpression?> = configuration.targets
        return targets.size == 1 && runContext.target.label.equals(targets.get(0))
    }
}
