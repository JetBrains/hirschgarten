/*
 * Copyright 2017 The Bazel Authors. All rights reserved.
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
import com.intellij.execution.Location
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.ogRun.other.BlazeCommandName
import java.util.*
import java.util.function.Function

/**
 * Handles the specific case where the user creates a run configuration by selecting test suites /
 * classes / methods from the test UI tree.
 *
 *
 * In this special case we already know the blaze target string, and only need to apply a filter
 * to the existing configuration. Delegates language-specific filter calculation to [ ].
 */
class BlazeFilterExistingRunConfigurationProducer :
  BlazeRunConfigurationProducer<BlazeCommandRunConfiguration?>(BlazeCommandRunConfigurationType.getInstance()) {
  override fun doSetupConfigFromContext(
    configuration: BlazeCommandRunConfiguration,
    context: ConfigurationContext,
    sourceElement: Ref<PsiElement?>?,
  ): Boolean {
    val testFilter: Optional<String?> = getTestFilter(context)
    if (!testFilter.isPresent()) {
      return false
    }
    val handlerState: BlazeCommandRunConfigurationCommonState? =
      configuration.getHandlerStateIfType<BlazeCommandRunConfigurationCommonState?>(
        BlazeCommandRunConfigurationCommonState::class.java,
      )
    if (handlerState == null ||
      !BlazeCommandName.TEST.equals(handlerState.commandState.getCommand())
    ) {
      return false
    }
    // replace old test filter flag if present
    val flags: MutableList<String?> = ArrayList<String?>(handlerState.blazeFlagsState.rawFlags)
    flags.removeIf { flag: String? -> flag.startsWith(BlazeFlags.TEST_FILTER) }
    flags.add(testFilter.get())

    if (SmRunnerUtils.countSelectedTestCases(context) == 1 &&
      !flags.contains(BlazeFlags.DISABLE_TEST_SHARDING)
    ) {
      flags.add(BlazeFlags.DISABLE_TEST_SHARDING)
    }
    handlerState.blazeFlagsState.rawFlags = flags
    configuration.setName(configuration.getName() + " (filtered)")
    configuration.setNameChangedByUser(true)
    return true
  }

  override fun doIsConfigFromContext(configuration: BlazeCommandRunConfiguration, context: ConfigurationContext): Boolean {
    val testFilter: Optional<String?> = getTestFilter(context)
    if (!testFilter.isPresent()) {
      return false
    }
    val handlerState: BlazeCommandRunConfigurationCommonState? =
      configuration.getHandlerStateIfType<BlazeCommandRunConfigurationCommonState?>(
        BlazeCommandRunConfigurationCommonState::class.java,
      )

    return handlerState != null &&
      handlerState.commandState.getCommand() == BlazeCommandName.TEST &&
      testFilter.get() == handlerState.testFilterFlag
  }

  companion object {
    private fun getTestFilter(context: ConfigurationContext): Optional<String?> {
      val base = context.getOriginalConfiguration(null)
      if (base !is BlazeCommandRunConfiguration) {
        return Optional.empty<String?>()
      }
      val targets: ImmutableList<Label?> =
        (base as BlazeCommandRunConfiguration).targets
      if (targets.isEmpty()) {
        return Optional.empty<String?>()
      }
      val selectedElements: MutableList<Location<*>?> = SmRunnerUtils.getSelectedSmRunnerTreeElements(context)
      if (selectedElements.isEmpty()) {
        return Optional.empty<String?>()
      }
      val testEventsHandler: Optional<BlazeTestEventsHandler?> =
        BlazeTestEventsHandler.getHandlerForTargets(context.getProject(), targets)
      return testEventsHandler.map<String?>(
        Function { handler: BlazeTestEventsHandler? ->
          handler.getTestFilter(
            context.getProject(),
            selectedElements,
          )
        },
      )
    }
  }
}
