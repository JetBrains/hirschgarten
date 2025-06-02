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
package org.jetbrains.bazel.run2.producers

import com.google.common.annotations.VisibleForTesting
import com.google.idea.blaze.base.sync.BlazeSyncModificationTracker
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.jetbrains.bazel.run2.BlazeCommandRunConfiguration
import org.jetbrains.bazel.run2.state.BlazeCommandRunConfigurationCommonState
import java.util.Arrays
import java.util.Objects

/** Produces run configurations via [TestContextProvider].  */
class TestContextRunConfigurationProducer :
  BlazeRunConfigurationProducer<BlazeCommandRunConfiguration>(BazelCommandRunConfigurationType) {
  private val cacheKey =
    Key.create<CachedValue<RunConfigurationContext>>(TestContextRunConfigurationProducer::class.java.getName() + "@" + this.hashCode())

  /** Implements [.equals] so that cached value stability checker passes.  */
  private class ContextWrapper(val context: ConfigurationContext) {
    override fun equals(obj: Any?): Boolean =
      obj is ContextWrapper &&
        context.psiLocation == obj.context.psiLocation

    override fun hashCode(): Int = Objects.hash(this.javaClass, context.psiLocation)
  }

  private fun findTestContext(context: ConfigurationContext): RunConfigurationContext? {
    if (!SmRunnerUtils.getSelectedSmRunnerTreeElements(context).isEmpty()) {
      // handled by a different producer
      return null
    }
    val wrapper = ContextWrapper(context)
    val psi = context.psiLocation
    return if (psi == null) {
      null
    } else {
      CachedValuesManager.getCachedValue(
        psi,
        cacheKey,
      ) {
        CachedValueProvider.Result.create(
          doFindTestContext(wrapper.context),
          PsiModificationTracker.MODIFICATION_COUNT,
          BlazeSyncModificationTracker.getInstance(wrapper.context.project),
        )
      }
    }
  }

  private fun doFindTestContext(context: ConfigurationContext): RunConfigurationContext? =
    Arrays
      .stream(TestContextProvider.EP_NAME.extensions)
      .map<RunConfigurationContext?> { p: TestContextProvider? -> p!!.getTestContext(context) }
      .filter { obj: RunConfigurationContext? -> Objects.nonNull(obj) }
      .findFirst()
      .orElse(null)

  override fun doSetupConfigFromContext(
    configuration: BlazeCommandRunConfiguration,
    context: ConfigurationContext,
    sourceElement: Ref<PsiElement?>,
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
    if (commonState.commandState.command != BlazeCommandName.TEST) {
      return false
    }
    val testContext = findTestContext(context)
    return testContext != null && testContext.matchesRunConfiguration(configuration)
  }
}
