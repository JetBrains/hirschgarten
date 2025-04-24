package org.jetbrains.bazel.java.run

import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.bazel.jpsCompilation.utils.JPS_COMPILED_BASE_DIRECTORY
import org.jetbrains.bazel.runnerAction.LocalJvmRunnerAction
import org.jetbrains.bsp.protocol.JvmEnvironmentItem

class BazelJvmEnvironmentProgramPatcher : JavaProgramPatcher() {
  override fun patchJavaParameters(
    executor: Executor,
    configuration: RunProfile,
    javaParameters: JavaParameters,
  ) {
    configuration.getEnvironment()?.let {
      val prioritizeIdeClasspath = configuration.includeJpsClassPaths() ?: false
      javaParameters.applyJavaParametersFromItem(it, prioritizeIdeClasspath)
    }
  }

  private fun RunProfile.getEnvironment() = (this as? UserDataHolderBase)?.getUserData(LocalJvmRunnerAction.jvmEnvironment)

  private fun RunProfile.includeJpsClassPaths() = (this as? UserDataHolderBase)?.getUserData(LocalJvmRunnerAction.includeJpsClassPaths)

  private fun JavaParameters.applyJavaParametersFromItem(item: JvmEnvironmentItem, includeJpsClassPaths: Boolean) {
    val newEnvironmentVariables = env + item.environmentVariables

    val jpsClassPaths = classPath.pathList.filter { it.contains(JPS_COMPILED_BASE_DIRECTORY) }

    val newClassPath =
      item.classpath.map { it.toString() } +
        if (includeJpsClassPaths) {
          jpsClassPaths
        } else {
          emptyList()
        }

    apply {
      env = newEnvironmentVariables
      classPath.clear()
      classPath.addAll(newClassPath)
      workingDirectory = item.workingDirectory.toString()
      vmParametersList.addAll(item.jvmOptions)
    }
  }
}
