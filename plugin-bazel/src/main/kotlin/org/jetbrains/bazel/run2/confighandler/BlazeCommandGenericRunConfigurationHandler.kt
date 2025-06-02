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
package org.jetbrains.bazel.run2.confighandler

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.commons.command.BlazeCommandName
import org.jetbrains.bazel.run2.BlazeCommandRunConfiguration
import org.jetbrains.bazel.run2.BlazeConfigurationNameBuilder
import org.jetbrains.bazel.run2.state.BlazeCommandRunConfigurationCommonState

/**
 * Generic handler for [BlazeCommandRunConfiguration]s, used as a fallback in the case where
 * no other handlers are more relevant.
 */
class BlazeCommandGenericRunConfigurationHandler
(configuration: BlazeCommandRunConfiguration) : BlazeCommandRunConfigurationHandler {
  override val state: BlazeCommandRunConfigurationCommonState = BlazeCommandRunConfigurationCommonState()

  override fun createRunner(executor: Executor, environment: ExecutionEnvironment): BlazeCommandRunConfigurationRunner =
    BlazeCommandGenericRunConfigurationRunner()

  @Throws(RuntimeConfigurationException::class)
  override fun checkConfiguration() {
    state.validate()
  }

  override fun suggestedName(configuration: BlazeCommandRunConfiguration): String? {
    if (configuration.targets.isEmpty()) {
      return null
    }
    return BlazeConfigurationNameBuilder(configuration).build()
  }

  override val commandName: BlazeCommandName?
    get() = state.commandState.command

  override val handlerName: String
    get() = "Generic Handler"
}
