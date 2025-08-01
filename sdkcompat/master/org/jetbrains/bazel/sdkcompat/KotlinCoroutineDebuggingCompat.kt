package org.jetbrains.bazel.sdkcompat

import com.intellij.debugger.engine.AsyncStacksUtils
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import org.jetbrains.bazel.annotations.RemoveWithSdkCompat
import org.jetbrains.kotlin.idea.debugger.coroutine.CoroutineAgentConnector
import java.util.concurrent.atomic.AtomicReference

@RemoveWithSdkCompat("v252")
val KOTLIN_COROUTINE_LIB_KEY: Key<AtomicReference<String>> = Key.create("bazel.debug.kotlin.coroutine.lib")

private fun createJvmFlag(flag: String) = "--jvmopt=$flag"

fun calculateKotlinCoroutineParams(environment: ExecutionEnvironment, project: Project): List<String> {
  val configuration = environment.runProfile as RunConfigurationBase<*>
  val javaParameters = JavaParameters()
  runReadAction {
    CoroutineAgentConnector.attachCoroutineAgent(project, configuration, javaParameters)
  }
  AsyncStacksUtils.addDebuggerAgent(javaParameters, project, false)
  return javaParameters.vmParametersList.parameters.map { createJvmFlag(it) }
}
