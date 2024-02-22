package org.jetbrains.plugins.bsp.runner

import ch.epfl.scala.bsp4j.JvmEnvironmentItem
import com.intellij.execution.Executor
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.runners.JavaProgramPatcher
import com.intellij.openapi.util.UserDataHolderBase
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.LocalJvmRunnerAction

public class BspJvmEnvironmentProgramPatcher : JavaProgramPatcher() {
  override fun patchJavaParameters(executor: Executor, configuration: RunProfile, javaParameters: JavaParameters) {
    configuration.getEnvironment()?.let {
      val prioritizeIdeClasspath = configuration.prioritizeIdeClasspath() ?: false
      javaParameters.applyJavaParametersFromItem(it, prioritizeIdeClasspath)
    }
  }

  private fun RunProfile.getEnvironment() =
    (this as? UserDataHolderBase)?.getUserData(LocalJvmRunnerAction.jvmEnvironment)

  private fun RunProfile.prioritizeIdeClasspath() =
    (this as? UserDataHolderBase)?.getUserData(LocalJvmRunnerAction.prioritizeIdeClasspath)

  private fun JavaParameters.applyJavaParametersFromItem(item: JvmEnvironmentItem, prioritizeIdeClassPath: Boolean) {
    val newEnvironmentVariables = env + item.environmentVariables

    val newClassPath =
      if (prioritizeIdeClassPath) classPath.pathList + item.classpath
      else item.classpath + classPath.pathList

    apply {
      env = newEnvironmentVariables
      classPath.clear()
      classPath.addAll(newClassPath)
      workingDirectory = item.workingDirectory
      vmParametersList.addAll(item.jvmOptions)
    }
  }
}
