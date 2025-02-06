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
package org.jetbrains.plugins.bsp.golang.run

import com.google.idea.blaze.base.command.BlazeCommandName

/** Go-specific handler for [BlazeCommandRunConfiguration]s.  */
class BlazeGoRunConfigurationHandler(configuration: BlazeCommandRunConfiguration) :
  BlazeCommandRunConfigurationHandler {
  private val buildSystem: BuildSystemName?
  private val state: BlazeCommandRunConfigurationCommonState

  init {
    this.buildSystem = Blaze.getBuildSystemName(configuration.getProject())
    this.state = BlazeCommandRunConfigurationCommonState(buildSystem)
  }

  public override fun getState(): BlazeCommandRunConfigurationCommonState {
    return state
  }

  public override fun createRunner(
    executor: com.intellij.execution.Executor?, environment: com.intellij.execution.runners.ExecutionEnvironment?
  ): BlazeCommandRunConfigurationRunner? {
    return BlazeGoRunConfigurationRunner()
  }

  @kotlin.Throws(com.intellij.execution.configurations.RuntimeConfigurationException::class)
  public override fun checkConfiguration() {
    state.validate(buildSystem)
  }

  public override fun suggestedName(configuration: BlazeCommandRunConfiguration): String? {
    if (configuration.getTargets().isEmpty()) {
      return null
    }
    return BlazeConfigurationNameBuilder(configuration).build()
  }

  val commandName: BlazeCommandName?
    get() = state.getCommandState().getCommand()

  val handlerName: String
    get() = "Go Handler"
}
