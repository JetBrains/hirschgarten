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
      javaParameters.applyJavaParametersFromItem(it)
    }
  }

  private fun RunProfile.getEnvironment() =
    (this as? UserDataHolderBase)?.getUserData(LocalJvmRunnerAction.jvmEnvironment)

  private fun JavaParameters.applyJavaParametersFromItem(item: JvmEnvironmentItem) {
    val newEnvironmentVariables = env + item.environmentVariables

    val oldClassPath = classPath.pathList
    val newClassPath = item.classpath + oldClassPath

    apply {
      env = newEnvironmentVariables
      classPath.clear()
      classPath.addAll(newClassPath)
      workingDirectory = item.workingDirectory
      vmParametersList.addAll(item.jvmOptions)
    }
  }
}
