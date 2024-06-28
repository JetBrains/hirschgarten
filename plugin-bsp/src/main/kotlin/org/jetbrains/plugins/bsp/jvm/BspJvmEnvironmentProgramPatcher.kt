package org.jetbrains.plugins.bsp.jvm

import ch.epfl.scala.bsp4j.JvmEnvironmentItem
import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.bsp.protocol.jpsCompilation.utils.JPS_COMPILED_BASE_DIRECTORY
import org.jetbrains.plugins.bsp.ui.actions.target.LocalJvmRunnerAction

public class BspJvmEnvironmentProgramPatcher : JavaProgramPatcher() {
  override fun patchJavaParameters(executor: Executor, configuration: RunProfile, javaParameters: JavaParameters) {
    configuration.getEnvironment()?.let {
      val prioritizeIdeClasspath = configuration.includeJpsClassPaths() ?: false
      javaParameters.applyJavaParametersFromItem(it, prioritizeIdeClasspath)
    }
  }

  private fun RunProfile.getEnvironment() =
    (this as? UserDataHolderBase)?.getUserData(LocalJvmRunnerAction.jvmEnvironment)

  private fun RunProfile.includeJpsClassPaths() =
    (this as? UserDataHolderBase)?.getUserData(LocalJvmRunnerAction.includeJpsClassPaths)

  private fun JavaParameters.applyJavaParametersFromItem(item: JvmEnvironmentItem, includeJpsClassPaths: Boolean) {
    val newEnvironmentVariables = env + item.environmentVariables

    val jpsClassPaths = classPath.pathList.filter { it.contains(JPS_COMPILED_BASE_DIRECTORY) }

    val newClassPath =
      if (includeJpsClassPaths) jpsClassPaths + item.classpath
      else item.classpath

    apply {
      env = newEnvironmentVariables
      classPath.clear()
      classPath.addAll(newClassPath)
      workingDirectory = item.workingDirectory
      vmParametersList.addAll(item.jvmOptions)
    }
  }
}
