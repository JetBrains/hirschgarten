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
package org.jetbrains.bazel.run2.coverage

import com.google.common.collect.ImmutableList
import com.google.idea.blaze.base.command.info.BlazeInfo
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.commons.command.BlazeCommandName
import org.jetbrains.bazel.run2.BlazeCommandRunConfiguration
import org.jetbrains.bazel.run2.ExecutorType
import org.jetbrains.bazel.run2.confighandler.BlazeCommandRunConfigurationRunner
import org.jetbrains.bazel.run2.state.BlazeCommandRunConfigurationCommonState
import java.io.File

/** Helper methods for coverage integration.  */
object CoverageUtils {
  fun coverageEnabled(env: ExecutionEnvironment): Boolean {
    return coverageEnabled(env.executor.id, env.runProfile)
  }

  fun coverageEnabled(executorId: String, profile: RunProfile?): Boolean {
    return ExecutorType.fromExecutorId(executorId) == ExecutorType.COVERAGE
        && isApplicableTo(profile)
  }

  fun isApplicableTo(runProfile: RunProfile?): Boolean {
    val config: BlazeCommandRunConfiguration? = toBlazeConfig(runProfile)
    if (config == null) {
      return false
    }
    val handlerState: BlazeCommandRunConfigurationCommonState? =
      config.getHandlerStateIfType(BlazeCommandRunConfigurationCommonState::class.java)
    if (handlerState == null) {
      return false
    }
    val command: BlazeCommandName? = handlerState.commandState.command
    return BlazeCommandName.TEST == command || BlazeCommandName.COVERAGE == command
  }

  private fun toBlazeConfig(profile: RunProfile): BlazeCommandRunConfiguration? {
    return BlazeCommandRunConfigurationRunner.getBlazeConfig(profile)
  }

  val blazeFlags: ImmutableList<String?> = ImmutableList.of<String?>("--combined_report=lcov")

  fun getOutputFile(blazeInfo: BlazeInfo): File {
    val coverageRoot: File = File(blazeInfo.getOutputPath(), "_coverage")
    return File(coverageRoot, "_coverage_report.dat")
  }
}
