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
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.bazel.ogRun.BlazeCommandRunConfiguration
import org.jetbrains.bazel.ogRun.other.BlazeCommandName
import org.jetbrains.bazel.ogRun.state.BlazeCommandRunConfigurationCommonState
import java.util.*

/** Produces run configurations via [TestContextProvider].  */
class TestContextRunConfigurationProducer :
  BlazeRunConfigurationProducer<BlazeCommandRunConfiguration>(BlazeCommandRunConfigurationType.getInstance()) {
  private val cacheKey =
    Key.create<CachedValue<RunConfigurationContext?>?>(TestContextRunConfigurationProducer::class.java.getName() + "@" + this.hashCode())

  /** Implements [.equals] so that cached value stability checker passes.  */
  private class ContextWrapper(val context: ConfigurationContext) {
    override fun equals(obj: Any?): Boolean =
      obj is ContextWrapper &&
        context.getPsiLocation() == obj.context.getPsiLocation()

    override fun hashCode(): Int = Objects.hash(this.javaClass, context.getPsiLocation())
  }

  private fun findTestContext(context: ConfigurationContext): RunConfigurationContext? {
    if (!SmRunnerUtils.getSelectedSmRunnerTreeElements(context).isEmpty()) {
      // handled by a different producer
      return null
    }
    val wrapper = ContextWrapper(context)
    val psi = context.getPsiLocation()
    return if (psi == null) {
      null
    } else {
      CachedValuesManager.getCachedValue<T?>(
        psi,
        cacheKey,
        CachedValueProvider {
          CachedValueProvider.Result.create<T?>(
            doFindTestContext(wrapper.context),
            PsiModificationTracker.MODIFICATION_COUNT,
            BlazeSyncModificationTracker.getInstance(wrapper.context.project),
          )
        },
      )
    }
  }

  private fun doFindTestContext(context: ConfigurationContext): RunConfigurationContext? =
    TestContextProvider.EP_NAME.extensionList.firstNotNullOfOrNull { it.getTestContext(context) }

  override fun doSetupConfigFromContext(
    configuration: BlazeCommandRunConfiguration,
    context: ConfigurationContext,
    sourceElement: Ref<PsiElement>,
  ): Boolean {
    val testContext = findTestContext(context) ?: return false
    if (!testContext.setupRunConfiguration(configuration)) {
      return false
    }
    sourceElement.set(testContext.sourceElement)
    return true
  }

  @VisibleForTesting
  public override fun doIsConfigFromContext(configuration: BlazeCommandRunConfiguration, context: ConfigurationContext): Boolean {
    val commonState: BlazeCommandRunConfigurationCommonState? =
      configuration.getHandlerStateIfType(
        BlazeCommandRunConfigurationCommonState::class.java,
      )
    if (commonState == null) {
      return false
    }
    if (commonState.commandState.getCommand() != BlazeCommandName.TEST) {
      return false
    }
    val testContext = findTestContext(context)
    return testContext != null && testContext.matchesRunConfiguration(configuration)
  }
}
