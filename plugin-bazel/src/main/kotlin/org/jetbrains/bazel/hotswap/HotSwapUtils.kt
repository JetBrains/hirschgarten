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
package org.jetbrains.bazel.hotswap

import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.settings.bazelProjectSettings
import org.jetbrains.plugins.bsp.runnerAction.BspJvmApplicationConfiguration

/** Helper methods for HotSwapping  */
object HotSwapUtils {
  fun canHotSwap(env: ExecutionEnvironment, project: Project): Boolean = project.isHotSwapEligible() && isDebugging(env)

  private fun Project.isHotSwapEligible(): Boolean = isBazelProject && bazelProjectSettings.hotSwapEnabled

  private fun isDebugging(environment: ExecutionEnvironment): Boolean =
    environment.executor is DefaultDebugExecutor &&
      environment.runProfile is BspJvmApplicationConfiguration
}
