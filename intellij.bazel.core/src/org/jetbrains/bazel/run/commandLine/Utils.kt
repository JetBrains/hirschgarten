package org.jetbrains.bazel.run.commandLine

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.util.execution.ParametersListUtil
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelHost
import org.jetbrains.bazel.commons.onBazelHost
import java.nio.file.Path

@ApiStatus.Internal
fun parseAsProgramArguments(value: String?): List<String> =
  if (value == null) emptyList()
  else ParametersListUtil.parse(
    /* parameterString = */ value,
    /* keepQuotes = */ false,
    /* supportSingleQuotes = */ true,
  )

// See: https://bazel.build/reference/test-encyclopedia
@ApiStatus.Internal
const val BAZEL_TEST_FILTER_ENV: String = "TESTBRIDGE_TEST_ONLY"

@ApiStatus.Internal
fun scriptPathCommandLine(
  scriptPath: Path,
  env: Map<String, String>,
  isTest: Boolean,
  isPassParentEnvs: Boolean,
  testFilter: String?,
  additionalScriptParameters: List<String>,
): GeneralCommandLine {
  val parentEnvironment = if (isTest || !isPassParentEnvs) {
    // Bazel tests don't receive the full environment because of sandboxing
    GeneralCommandLine.ParentEnvironmentType.NONE
  }
  else {
    GeneralCommandLine.ParentEnvironmentType.CONSOLE
  }
  // Bazel wrote the script in its own environment, for example in WSL, and the script must run there.
  val host = BazelHost.of(scriptPath)
  val commandLine =
    GeneralCommandLine()
      .withExePath(host.toHostPath(scriptPath))
      .withParentEnvironmentType(parentEnvironment)
      .withEnvironment(env)
      .withParameters(additionalScriptParameters)
      .onBazelHost(host, inheritHostEnvironment = parentEnvironment != GeneralCommandLine.ParentEnvironmentType.NONE)
  if (testFilter != null) {
    commandLine.environment[BAZEL_TEST_FILTER_ENV] = testFilter
  }
  return commandLine
}
