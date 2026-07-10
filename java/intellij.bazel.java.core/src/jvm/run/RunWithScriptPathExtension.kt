package org.jetbrains.bazel.jvm.run

import com.intellij.execution.Executor
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.run.config.BazelRunConfiguration
import org.jetbrains.bazel.run.state.HasRunWithBazel

@ApiStatus.Internal
interface RunWithScriptPathExtension {
  /**
   * Return `true` if `bazel run --script_path=foo //foo && ./foo` should be used instead of calling Bazel directly
   * for this [executor] and [configuration].
   * @see org.jetbrains.bazel.jvm.run.ScriptPathBeforeRunTaskProvider
   */
  fun shouldRunWithScriptPath(executor: Executor, configuration: BazelRunConfiguration): Boolean

  companion object {
    fun shouldRunWithScriptPath(executor: Executor, configuration: BazelRunConfiguration): Boolean =
      // Because `bazel run` only supports one target, so does `bazel run --script_path`
      configuration.targets.size == 1 &&
      ep.extensionList.any { it.shouldRunWithScriptPath(executor, configuration) }

    private val ep: ExtensionPointName<RunWithScriptPathExtension> = ExtensionPointName("org.jetbrains.bazel.runWithScriptPathExtension")
  }
}

/**
 * If [HasRunWithBazel.runWithBazel] was set to `false` by the user, use --script_path instead of `bazel run/test` with [DefaultRunExecutor]
 */
internal class DefaultRunWithScriptPathExtension : RunWithScriptPathExtension {
  override fun shouldRunWithScriptPath(
    executor: Executor,
    configuration: BazelRunConfiguration,
  ): Boolean =
    executor is DefaultRunExecutor && (configuration.handler?.state as? HasRunWithBazel)?.runWithBazel == false
}

/**
 * Always run with --script_path when debugging, because:
 * - With remote execution, our Java debug port will be open on the remote server instead of locally
 * - `--script_path` doesn't hold Bazel's lock, allowing hotswap together with debug.
 *   This is impossible with plain `bazel run` as you can't run `bazel build` in parallel.
 * - Calling `bazel run` may involve compilation, causing debugger attachment to time out.
 */
internal class DebugRunWithScriptPathExtension : RunWithScriptPathExtension {
  override fun shouldRunWithScriptPath(
    executor: Executor,
    configuration: BazelRunConfiguration,
  ): Boolean = executor is DefaultDebugExecutor
}
