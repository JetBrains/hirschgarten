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
package org.jetbrains.bazel.ogRun.coverage


import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.ogRun.BlazeCommandRunConfiguration
import org.jetbrains.bazel.ogRun.ExecutorType
import org.jetbrains.bazel.ogRun.other.BlazeCommandName
import org.jetbrains.bazel.ogRun.state.BlazeCommandRunConfigurationCommonState
import java.io.File

/** Helper methods for coverage integration.  */
object CoverageUtils {
  fun coverageEnabled(env: ExecutionEnvironment): Boolean = coverageEnabled(env.executor.id, env.getRunProfile())

  fun coverageEnabled(executorId: String, profile: RunProfile): Boolean =
    ExecutorType.fromExecutorId(executorId) == ExecutorType.COVERAGE &&
      isApplicableTo(profile)

  fun isApplicableTo(runProfile: RunProfile): Boolean {
    val config: BlazeCommandRunConfiguration = toBlazeConfig(runProfile) ?: return false
    val handlerState: BlazeCommandRunConfigurationCommonState? =
      config.getHandlerStateIfType(
        BlazeCommandRunConfigurationCommonState::class.java,
      )
    if (handlerState == null) {
      return false
    }
    val command: BlazeCommandName = handlerState.commandState.getCommand()
    return BlazeCommandName.TEST == command || BlazeCommandName.COVERAGE == command
  }

  private fun toBlazeConfig(profile: RunProfile): BlazeCommandRunConfiguration? =
    BlazeCommandRunConfigurationRunner.getBlazeConfig(profile)

  val blazeFlags: List<String?> = listOf<String?>("--combined_report=lcov")

  fun getOutputFile(blazeInfo: BlazeInfo): File {
    val coverageRoot: File = File(blazeInfo.getOutputPath(), "_coverage")
    return File(coverageRoot, "_coverage_report.dat")
  }
}
