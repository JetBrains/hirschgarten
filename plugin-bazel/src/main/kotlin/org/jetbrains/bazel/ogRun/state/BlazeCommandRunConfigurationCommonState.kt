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
package org.jetbrains.bazel.ogRun.state

import com.google.common.base.Preconditions
import com.google.common.collect.ImmutableList
import com.google.idea.blaze.base.command.BlazeFlags
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.openapi.project.Project
import java.io.File

/**
 * Shared state common to several [ ] types.
 */
class BlazeCommandRunConfigurationCommonState : RunConfigurationCompositeState() {
  val commandState: BlazeCommandState

  /** @return The list of blaze flags that the user specified manually.
   */
  val blazeFlagsState: RunConfigurationFlagsState

  /** @return The list of executable flags the user specified manually.
   */
  val exeFlagsState: RunConfigurationFlagsState

  /** @return The environment variables the user specified manually.
   */
  val userEnvVarsState: EnvironmentVariablesState
  val blazeBinaryState: BlazeBinaryState

  init {
    this.commandState = BlazeCommandState()
    this.blazeFlagsState = RunConfigurationFlagsState(USER_BLAZE_FLAG_TAG, buildSystemName.toString() + " flags:")
    this.exeFlagsState = RunConfigurationFlagsState(USER_EXE_FLAG_TAG, "Executable flags:")
    this.userEnvVarsState = EnvironmentVariablesState()
    this.blazeBinaryState = BlazeBinaryState()
  }

  override fun initializeStates(): ImmutableList<RunConfigurationState?> =
    ImmutableList.of<RunConfigurationState?>(
      this.commandState,
      this.blazeFlagsState,
      this.exeFlagsState,
      this.userEnvVarsState,
      this.blazeBinaryState,
    )

  val testFilterFlag: String?
    /** Searches through all blaze flags for the first one beginning with '--test_filter'  */
    get() {
      for (flag in this.blazeFlagsState.getRawFlags()) {
        if (flag.startsWith(BlazeFlags.TEST_FILTER)) {
          return flag
        }
      }
      return null
    }

  val testFilterForExternalProcesses: String?
    /**
     * The actual test filter value intended to be passed directly to external processes or
     * environment variables.
     *
     *
     * Unlike [.getTestFilterFlag], this is not a flag intended to be used on the command
     * line, so the shell-escaping/quoting has been removed.
     */
    get() {
      val testFilterFlag =
        this.blazeFlagsState
          .getFlagsForExternalProcesses()
          .stream()
          .filter { flag: String? -> flag.startsWith(BlazeFlags.TEST_FILTER) }
          .findFirst()
          .orElse(null)
      if (testFilterFlag == null) {
        return null
      }

      Preconditions.checkState(
        testFilterFlag.startsWith(
          TEST_FILTER_FLAG_PREFIX,
        ),
      )
      return testFilterFlag.substring(TEST_FILTER_FLAG_PREFIX.length)
    }

  val testArgs: ImmutableList<String?>?
    get() =
      this.blazeFlagsState
        .getRawFlags()
        .stream()
        .filter { f: String? -> f.startsWith(BlazeFlags.TEST_ARG) }
        .map<String?> { f: String? -> f.substring(BlazeFlags.TEST_ARG.length()) }
        .collect(ImmutableList.toImmutableList<String?>())

  @Throws(RuntimeConfigurationException::class)
  fun validate(buildSystemName: BuildSystemName) {
    if (this.commandState.getCommand() == null) {
      throw RuntimeConfigurationError("You must specify a command.")
    }
    val blazeBinaryString = this.blazeBinaryState.blazeBinary
    if (blazeBinaryString != null && !(File(blazeBinaryString).exists())) {
      throw RuntimeConfigurationError(buildSystemName.getName() + " binary does not exist")
    }
  }

  override fun getEditor(project: Project): RunConfigurationStateEditor = RunConfigurationCompositeStateEditor(project, getStates())

  companion object {
    private const val USER_BLAZE_FLAG_TAG = "blaze-user-flag"
    private const val USER_EXE_FLAG_TAG = "blaze-user-exe-flag"
    private val TEST_FILTER_FLAG_PREFIX: String = BlazeFlags.TEST_FILTER + '='
  }
}
