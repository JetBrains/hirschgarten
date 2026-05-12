package com.intellij.bazel.java.profiler

import com.intellij.execution.Executor
import com.intellij.profiler.DefaultProfilerExecutorGroup
import org.jetbrains.bazel.jvm.run.RunWithScriptPathExtension
import org.jetbrains.bazel.run.config.BazelRunConfiguration

/**
 * Always run with `--script_path` for Profiler,
 * because it makes sure the process is run locally (unlike with `bazel run` and remote execution),
 * and because we can add JVM arguments to the script without invalidating Bazel's analysis cache
 */
internal class ProfilerRunWithScriptPathExtension : RunWithScriptPathExtension {
  override fun shouldRunWithScriptPath(
    executor: Executor,
    configuration: BazelRunConfiguration,
  ): Boolean =
    isProfilerExecutor(executor.id)

  private fun isProfilerExecutor(executorId: String): Boolean =
    DefaultProfilerExecutorGroup.getInstance()?.getRegisteredSettings(executorId)?.state != null
}
