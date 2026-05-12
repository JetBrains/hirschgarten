package org.jetbrains.bazel.java.coverage

import com.intellij.coverage.CoverageExecutor
import com.intellij.execution.Executor
import org.jetbrains.bazel.jvm.run.RunWithScriptPathExtension
import org.jetbrains.bazel.run.config.BazelRunConfiguration

/**
 * Run with `--script_path` if using Java coverage with an agent,
 * because we can add JVM arguments to the script without invalidating Bazel's analysis cache
 * and because adding a local Java agent isn't possible with remote execution
 */
internal class CoverageRunWithScriptPathExtension : RunWithScriptPathExtension {
  override fun shouldRunWithScriptPath(
    executor: Executor,
    configuration: BazelRunConfiguration,
  ): Boolean = executor is CoverageExecutor && isJavaAgentCoverageApplicableTo(configuration)
}
