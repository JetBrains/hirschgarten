package org.jetbrains.bazel.golang.debug

import com.goide.execution.GoBuildingRunConfiguration
import com.goide.execution.GoBuildingRunningState
import com.goide.execution.application.GoApplicationConfiguration
import com.goide.execution.application.GoApplicationRunningState
import com.goide.execution.testing.GoTestRunConfiguration
import com.goide.execution.testing.GoTestRunningState
import com.goide.util.GoCommandLineParameter
import com.goide.util.GoExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import org.jetbrains.bazel.run.state.EnvironmentVariablesDataOptions
import org.jetbrains.bazel.run.state.GenericRunState
import org.jetbrains.bazel.run.state.GenericTestState
import kotlin.collections.set
import kotlin.io.path.pathString

internal interface GoDebugCommandLineState {

  fun patchNativeState()
}

internal open class GoRunWithDebugCommandLineState(
  val environment: ExecutionEnvironment,
  module: Module,
  configuration: GoApplicationConfiguration,
  private val settings: GenericRunState,
) : GoApplicationRunningState(environment, module, configuration), GoDebugCommandLineState {
  override fun isDebug(): Boolean = true

  override fun getBuildingTarget(): List<GoCommandLineParameter>? = null

  override fun createBuildExecutor(): GoExecutor? = null

  override fun patchNativeState() {
    val executableInfo = environment.getCopyableUserData(EXECUTABLE_KEY)?.get()
    if (executableInfo != null) applyExecutableInfo(executableInfo)
    applyState(settings.env, settings.programArguments)
  }
}

internal class GoTestWithDebugCommandLineState(
  val environment: ExecutionEnvironment,
  module: Module,
  private val configuration: GoTestRunConfiguration,
  private val settings: GenericTestState,
) : GoTestRunningState(environment, module, configuration), GoDebugCommandLineState {
  override fun isDebug(): Boolean = true

  override fun getBuildingTarget(): List<GoCommandLineParameter>? = null

  override fun createBuildExecutor(): GoExecutor? = null

  override fun patchNativeState() {
    val executableInfo = environment.getCopyableUserData(EXECUTABLE_KEY)?.get()
    if (executableInfo != null) applyExecutableInfo(executableInfo)
    applyState(settings.env, settings.programArguments)
    settings.testFilter?.let { configuration.customEnvironment["TESTBRIDGE_TEST_ONLY"] = it }
    // GO_TEST_WRAP=0 ensures the tests are run in the debugged process, not a child process
    configuration.customEnvironment["GO_TEST_WRAP"] = "0"
  }
}

private fun GoBuildingRunningState<*>.applyExecutableInfo(executableInfo: ExecutableInfo) {
  with(configuration) {
    for ((key, value) in executableInfo.envVars) {
      customEnvironment[key] = value
    }
    kind = GoBuildingRunConfiguration.Kind.PACKAGE
    outputDirectory = executableInfo.binary.parent?.pathString
    workingDirectory = executableInfo.workingDir.pathString
  }
  setOutputFilePath(executableInfo.binary.pathString)
}

private fun GoBuildingRunningState<*>.applyState(
  env: EnvironmentVariablesDataOptions,
  programArguments: String?
) = with(configuration) {
  for ((key, value) in env.envs) {
    customEnvironment[key] = value
  }
  isPassParentEnvironment = env.isPassParentEnvs
  programArguments?.let { params = it }
}
