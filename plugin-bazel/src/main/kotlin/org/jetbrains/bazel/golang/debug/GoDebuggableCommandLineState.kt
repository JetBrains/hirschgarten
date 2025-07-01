package org.jetbrains.bazel.golang.debug

import com.goide.execution.GoBuildingRunConfiguration
import com.goide.execution.application.GoApplicationConfiguration
import com.goide.execution.application.GoApplicationRunningState
import com.goide.util.GoCommandLineParameter
import com.goide.util.GoExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.module.Module
import org.jetbrains.bazel.taskEvents.OriginId

open class GoDebuggableCommandLineState(
  val environment: ExecutionEnvironment,
  module: Module,
  configuration: GoApplicationConfiguration,
  protected val originId: OriginId,
) : GoApplicationRunningState(
    environment,
    module,
    configuration,
  ) {
  override fun isDebug(): Boolean = true

  override fun getBuildingTarget(): List<GoCommandLineParameter>? = null

  override fun createBuildExecutor(): GoExecutor? = null

  fun patchNativeState() {
    val executableInfo = environment.getCopyableUserData(EXECUTABLE_KEY)?.get()
    if (executableInfo != null) {
      with(configuration) {
        for (envVar in executableInfo.envVars) {
          customEnvironment[envVar.key] = envVar.value
        }
        kind = GoBuildingRunConfiguration.Kind.PACKAGE
        outputDirectory = executableInfo.binary.parent
        workingDirectory = executableInfo.workingDir.path
      }
      setOutputFilePath(executableInfo.binary.path)
    }
    patchAdditionalParams()
  }

  open fun patchAdditionalParams() {}
}
