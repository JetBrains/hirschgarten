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

import com.google.common.base.Preconditions
import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.WrappingRunConfiguration
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.util.Key
import org.jetbrains.bazel.commons.command.BlazeCommandName
import org.jetbrains.bazel.run2.BlazeCommandRunConfiguration
import org.jetbrains.bazel.run2.state.BlazeCommandRunConfigurationCommonState

/**
 * Supports the execution of [BlazeCommandRunConfiguration]s.
 *
 *
 * Provides rule-specific RunProfileState and before-run-tasks.
 */
interface BlazeCommandRunConfigurationRunner {
  /** @return the RunProfileState corresponding to the given environment.
   */
  @Throws(ExecutionException::class)
  fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState?

  /**
   * Executes any required before run tasks.
   *
   * @return true if no task exists or the task was successfully completed. Otherwise returns false
   * if the task either failed or was cancelled.
   */
  fun executeBeforeRunTask(environment: ExecutionEnvironment): Boolean

  companion object {
    fun isDebugging(environment: ExecutionEnvironment): Boolean {
      val executor = environment.executor
      return executor is DefaultDebugExecutor
    }

    @JvmStatic
    fun getConfiguration(environment: ExecutionEnvironment): BlazeCommandRunConfiguration {
      val runProfile = environment.runProfile
      return Preconditions.checkNotNull<BlazeCommandRunConfiguration>(getBlazeConfig(runProfile))
    }

    @JvmStatic
    fun getBlazeConfig(runProfile: RunProfile): BlazeCommandRunConfiguration? {
      var runProfile = runProfile
      if (runProfile is WrappingRunConfiguration<*>) {
        runProfile = (runProfile as WrappingRunConfiguration<*>).getPeer()
      }
      return if (runProfile is BlazeCommandRunConfiguration)
        runProfile as BlazeCommandRunConfiguration
      else
        null
    }

    fun getBlazeCommand(environment: ExecutionEnvironment): BlazeCommandName? {
      val config: BlazeCommandRunConfiguration = getConfiguration(environment)
      val commonState: BlazeCommandRunConfigurationCommonState? =
        config.getHandlerStateIfType(BlazeCommandRunConfigurationCommonState::class.java)
      return commonState?.commandState?.command
    }

    /** Used to store a runner to an [ExecutionEnvironment].  */
    @JvmField
    val RUNNER_KEY: Key<BlazeCommandRunConfigurationRunner?> =
      Key.create<BlazeCommandRunConfigurationRunner?>("blaze.run.config.runner")
  }
}
