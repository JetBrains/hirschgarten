package org.jetbrains.bazel.run.commandLine

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.util.execution.ParametersListUtil
import org.jetbrains.annotations.ApiStatus
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
  val commandLine =
    GeneralCommandLine()
      .withExePath(scriptPath.toString())
      .withParentEnvironmentType(parentEnvironment)
      .withEnvironment(env)
      .withParameters(additionalScriptParameters)
  if (testFilter != null) {
    commandLine.environment[BAZEL_TEST_FILTER_ENV] = testFilter
  }
  return commandLine
}
