package org.jetbrains.bazel.sdkcompat

import com.intellij.debugger.engine.AsyncStacksUtils
import com.intellij.debugger.impl.GenericDebuggerRunnerSettings
import com.intellij.execution.JavaRunConfigurationExtensionManager
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.application.readAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Key
import org.jetbrains.bazel.annotations.RemoveWithSdkCompat
import java.util.concurrent.atomic.AtomicReference

private fun createJvmFlag(flag: String) = "--jvmopt=$flag"

val COROUTINE_JVM_FLAGS_KEY = Key.create<AtomicReference<List<String>>>("bazel.coroutine.jvm.flags")

/**
 * this method should be moved to the main codebase in [KotlinCoroutineLibraryFinderBeforeRunTaskProvider] after the complete removal of v252
 */
@RemoveWithSdkCompat("v252")
suspend fun calculateKotlinCoroutineParams(environment: ExecutionEnvironment, module: Module) {
  val javaParameters = createJavaParameters(environment)
  val jvmFlags = toJvmFlags(javaParameters.vmParametersList.parameters)
  environment.getCopyableUserData(COROUTINE_JVM_FLAGS_KEY)?.set(jvmFlags)
}

private suspend fun createJavaParameters(environment: ExecutionEnvironment): JavaParameters {
  val configuration = environment.runProfile as RunConfigurationBase<*>
  val parameters = JavaParameters()
  val runnerSettings = environment.runnerSettings ?: GenericDebuggerRunnerSettings()

  readAction {
    JavaRunConfigurationExtensionManager.instance
      .updateJavaParameters(configuration, parameters, runnerSettings, environment.executor)
  }

  AsyncStacksUtils.addDebuggerAgent(parameters, environment.project, false)
  return parameters
}

private fun toJvmFlags(javaParameters: List<String>): List<String> = javaParameters.map { createJvmFlag(it) }
