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
package org.jetbrains.bazel.ogRun.confighandler

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.bazel.ogRun.BlazeCommandRunConfiguration
import org.jetbrains.bazel.ogRun.ExecutorType
import org.jetbrains.bazel.ogRun.PendingRunConfigurationContext
import org.jetbrains.bazel.ogRun.other.BlazeCommandName
import org.jetbrains.bazel.ogRun.state.BlazeCommandRunConfigurationCommonState
import com.intellij.execution.RunManager;


class PendingTargetRunConfigurationHandler internal constructor(config: BlazeCommandRunConfiguration) :
  BlazeCommandRunConfigurationHandler {
    override val state: BlazeCommandRunConfigurationCommonState

    init {
      this.buildSystemName = Blaze.getBuildSystemName(config.project)
      this.state = BlazeCommandRunConfigurationCommonState(buildSystemName)
    }

    override fun createRunner(executor: Executor, environment: ExecutionEnvironment): BlazeCommandRunConfigurationRunner =
      PendingTargetRunner()

    @Throws(RuntimeConfigurationException::class)
    override fun checkConfiguration() {
      state.validate()
    }

    public override fun suggestedName(configuration: BlazeCommandRunConfiguration): String? = null

    override val commandName: BlazeCommandName
      get() = state.commandState.getCommand()

    override val handlerName: String
      get() = "Pending target handler"

    internal class PendingTargetProgramRunner : ProgramRunner<RunnerSettings> {
      override fun getRunnerId(): String = "PendingTargetProgramRunner"

      override fun canRun(executorId: String, profile: RunProfile): Boolean {
        val config: BlazeCommandRunConfiguration =
          BlazeCommandRunConfigurationRunner.getBlazeConfig(profile)
        val type: ExecutorType = ExecutorType.fromExecutorId(executorId)
        val pendingContext: PendingRunConfigurationContext? = config.getPendingContext()
        return pendingContext != null &&
          !pendingContext.isDone &&
          pendingContext
            .supportedExecutors()
            .contains(type)
      }

      @Throws(ExecutionException::class)
      override fun execute(env: ExecutionEnvironment) {
        if (env.state !is DummyRunProfileState) {
          reRunConfiguration(env)
          return
        }
        ApplicationManager
          .getApplication()
          .executeOnPooledThread(
            Runnable {
              try {
                resolveContext(env)
              } catch (e: ExecutionException) {
                ExecutionUtil.handleExecutionError(env, e)
              }
            },
          )
      }
    }

    /**
     * A placeholder [RunProfileState]. This is bypassed entirely by PendingTargetProgramRunner.
     */
    private class DummyRunProfileState : RunProfileState {
      override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult =
        throw RuntimeException("Unexpected code path")
    }

    private class PendingTargetRunner : BlazeCommandRunConfigurationRunner {
      override fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState = DummyRunProfileState()

      override fun executeBeforeRunTask(environment: ExecutionEnvironment): Boolean {
        // if we got here, a different ProgramRunner has accepted a pending blaze run config, despite
        // PendingTargetProgramRunner being first in the list
        throw RuntimeException(
          String.format(
            "Unexpected code path: program runner %s, executor: %s",
            environment.runner.javaClass,
            environment.executor.id,
          ),
        )
      }
    }

    companion object {
      @Throws(ExecutionException::class)
      private fun reRunConfiguration(env: ExecutionEnvironment) {
        val config: BlazeCommandRunConfiguration = BlazeCommandRunConfigurationRunner.getConfiguration(env)
        val settings: RunnerAndConfigurationSettings =
          RunManager.getInstance(config.project).findSettings(config)
            ?: throw ExecutionException(
              "Can't find runner settings for blaze run configuration " + config.name,
            )
        // TODO(brendandouglas): check the executor type and inform the user if it's not applicable to
        // this target
        RunManager.getInstance(env.project).selectedConfiguration = settings
        ExecutionUtil.runConfiguration(settings, env.executor)
      }

      @Throws(ExecutionException::class)
      private fun resolveContext(env: ExecutionEnvironment) {
        val config: BlazeCommandRunConfiguration = BlazeCommandRunConfigurationRunner.getConfiguration(env)
        val pendingContext: PendingRunConfigurationContext = config.getPendingContext() ?: return
        pendingContext.resolve(
          env,
          config,
        ) {
          try {
            reRunConfiguration(env)
          } catch (e: ExecutionException) {
            ExecutionUtil.handleExecutionError(env, e)
          }
        }
      }
    }
  }
