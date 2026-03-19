package org.jetbrains.bazel.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.annotations.ApiStatus

/**
 * Supports the run configuration flow for BSP run configurations.
 * The lifetime of a handler is shorter than the lifetime of a run configuration and fully owned by the run configuration.
 *
 * <p>Provides language-specific configuration state, validation, presentation, and runner.
 */
@ApiStatus.Internal
interface BazelRunHandler {
  val state: BazelRunConfigurationState<*>

  /**
   * The name of the run handler (shown in the UI).
   */
  val name: String

  val isTestHandler: Boolean

  fun getRunProfileState(executor: Executor, environment: ExecutionEnvironment): RunProfileState
}
