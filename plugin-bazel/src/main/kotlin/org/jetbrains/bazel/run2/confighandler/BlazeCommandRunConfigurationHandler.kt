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

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.commons.command.BlazeCommandName
import org.jetbrains.bazel.run2.BlazeCommandRunConfiguration
import org.jetbrains.bazel.run2.state.RunConfigurationState

/**
 * Supports the run configuration flow for [BlazeCommandRunConfiguration]s.
 *
 *
 * Provides rule-specific configuration state, validation, presentation, and runner.
 */
interface BlazeCommandRunConfigurationHandler {
  val state: RunConfigurationState

  /** @return A [BlazeCommandRunConfigurationRunner] for running the configuration.
   */
  @Throws(ExecutionException::class)
  fun createRunner(executor: Executor, environment: ExecutionEnvironment): BlazeCommandRunConfigurationRunner

  /**
   * Checks whether the handler settings are valid.
   *
   * @throws RuntimeConfigurationException for configuration errors the user should be warned about.
   */
  @Throws(RuntimeConfigurationException::class)
  fun checkConfiguration()

  /**
   * @return The default name of the run configuration based on its settings and this handler's
   * state.
   */
  fun suggestedName(configuration: BlazeCommandRunConfiguration): String?

  /**
   * @return The [BlazeCommandName] associated with this state. May be null if no command is
   * set.
   */
  val commandName: BlazeCommandName?

  /** @return The name of this handler. Shown in the UI.
   */
  val handlerName: String
}
