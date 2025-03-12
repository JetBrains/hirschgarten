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
package org.jetbrains.bazel.ogRun

import com.google.idea.blaze.base.settings.Blaze
import javax.swing.Icon

/**
 * Provides a before run task provider that immediately transfers control to [ ]
 */
internal class BlazeBeforeRunTaskProvider

    : com.intellij.execution.BeforeRunTaskProvider<BlazeBeforeRunTaskProvider.Task>() {
    internal class Task private constructor() : com.intellij.execution.BeforeRunTask<Task>(ID) {
        init {
            setEnabled(true)
        }
    }

    val id: com.intellij.openapi.util.Key<Task?>
        get() = ID

    val icon: Icon?
        get() = BlazeIcons.Logo

    override fun getTaskIcon(task: Task?): Icon? {
        return BlazeIcons.Logo
    }

    val name: String = "Bazel before-run task"

    override fun getDescription(task: Task?): String = name

    val isConfigurable: Boolean
        get() = false

    override fun createTask(config: com.intellij.execution.configurations.RunConfiguration?): Task? {
        if (config is BlazeCommandRunConfiguration) {
            return BlazeBeforeRunTaskProvider.Task()
        }
        return null
    }

    override fun configureTask(
        runConfiguration: com.intellij.execution.configurations.RunConfiguration?,
        task: Task?
    ): Boolean {
        return false
    }

    override fun canExecuteTask(
        configuration: com.intellij.execution.configurations.RunConfiguration?,
        task: Task?
    ): Boolean {
        var configuration: com.intellij.execution.configurations.RunConfiguration? = configuration
        if (configuration is com.intellij.execution.configurations.WrappingRunConfiguration<*>) {
            configuration =
                (configuration as com.intellij.execution.configurations.WrappingRunConfiguration<*>).getPeer()
        }
        return configuration is BlazeCommandRunConfiguration
    }

    override fun executeTask(
        dataContext: com.intellij.openapi.actionSystem.DataContext?,
        configuration: com.intellij.execution.configurations.RunConfiguration?,
        env: com.intellij.execution.runners.ExecutionEnvironment,
        task: Task?
    ): Boolean {
        if (!canExecuteTask(configuration, task)) {
            return false
        }
        val runner: BlazeCommandRunConfigurationRunner =
            env.getCopyableUserData<BlazeCommandRunConfigurationRunner>(BlazeCommandRunConfigurationRunner.RUNNER_KEY)
        try {
            return runner.executeBeforeRunTask(env)
        } catch (e: RuntimeException) {
            // An uncaught exception here means IntelliJ never cleans up and the configuration is always
            // considered to be already running, so you can't start it ever again.
            logger.warn("Uncaught exception in Blaze before run task", e)
            com.intellij.execution.runners.ExecutionUtil.handleExecutionError(
                env,
                com.intellij.execution.ExecutionException(e)
            )
            return false
        }
    }

    companion object {
        private val logger: com.intellij.openapi.diagnostic.Logger =
            com.intellij.openapi.diagnostic.Logger.getInstance(BlazeBeforeRunTaskProvider::class.java)

        @JvmField
        val ID: com.intellij.openapi.util.Key<Task?> =
            com.intellij.openapi.util.Key.create<Task?>("Blaze.BeforeRunTask")
    }
}
