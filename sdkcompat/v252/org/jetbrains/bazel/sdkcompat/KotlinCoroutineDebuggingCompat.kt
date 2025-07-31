package org.jetbrains.bazel.sdkcompat

import com.intellij.debugger.engine.AsyncStacksUtils
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import java.util.concurrent.atomic.AtomicReference

private fun createJvmFlag(flag: String) = "--jvmopt=$flag"

private fun createJavaAgentFlag(jarPath: String) = createJvmFlag("-javaagent:$jarPath")

val KOTLIN_COROUTINE_LIB_KEY: Key<AtomicReference<String>> = Key.create("bazel.debug.kotlin.coroutine.lib")

fun calculateKotlinCoroutineParams(environment: ExecutionEnvironment, project: Project): List<String> {
  val javaParameters = JavaParameters()
  AsyncStacksUtils.addDebuggerAgent(javaParameters, project, false)
  return listOfNotNull(
    environment.getCopyableUserData(KOTLIN_COROUTINE_LIB_KEY)?.get()?.let { createJavaAgentFlag(it) },
  ) + javaParameters.vmParametersList.parameters.map { createJvmFlag(it) }
}
