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
package org.jetbrains.bazel.run2

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.WrappingRunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Key
import org.jetbrains.bazel.assets.BazelPluginIcons
import org.jetbrains.bazel.run2.confighandler.BlazeCommandRunConfigurationRunner
import javax.swing.Icon

/**
 * Provides a before run task provider that immediately transfers control to [ ]
 */
class BlazeBeforeRunTaskProvider
  : BeforeRunTaskProvider<BlazeBeforeRunTaskProvider.Task>() {
  class Task() : BeforeRunTask<Task>(ID) {
    init {
      isEnabled = true
    }
  }

  override fun getId(): Key<Task> {
    return ID
  }

  override fun getIcon(): Icon {
    return BazelPluginIcons.bazel
  }

  override fun getTaskIcon(task: Task): Icon {
    return BazelPluginIcons.bazel
  }

  override fun getName(): String {
    return "Bazel" + " before-run task"
  }

  override fun getDescription(task: Task): String {
    return "Bazel" + " before-run task"
  }

  override fun isConfigurable(): Boolean {
    return false
  }

  override fun createTask(config: RunConfiguration): Task? {
    if (config is BlazeCommandRunConfiguration) {
      return Task()
    }
    return null
  }

  override fun configureTask(runConfiguration: RunConfiguration, task: Task): Boolean {
    return false
  }

  override fun canExecuteTask(configuration: RunConfiguration, task: Task): Boolean {
    var configuration = configuration
    if (configuration is WrappingRunConfiguration<*>) {
      configuration = (configuration as WrappingRunConfiguration<*>).getPeer()
    }
    return configuration is BlazeCommandRunConfiguration
  }

  override fun executeTask(
    dataContext: DataContext,
    configuration: RunConfiguration,
    env: ExecutionEnvironment,
    task: Task
  ): Boolean {
    if (!canExecuteTask(configuration, task)) {
      return false
    }
    val runner: BlazeCommandRunConfigurationRunner? =
      env.getCopyableUserData(BlazeCommandRunConfigurationRunner.RUNNER_KEY)
    try {
      return runner?.executeBeforeRunTask(env) == true
    } catch (e: RuntimeException) {
      // An uncaught exception here means IntelliJ never cleans up and the configuration is always
      // considered to be already running, so you can't start it ever again.
      logger.warn("Uncaught exception in Blaze before run task", e)
      ExecutionUtil.handleExecutionError(env, ExecutionException(e))
      return false
    }
  }

  companion object {
    private val logger = Logger.getInstance(BlazeBeforeRunTaskProvider::class.java)

    @JvmField
    val ID: Key<Task> = Key.create<Task>("Blaze.BeforeRunTask")
  }
}
