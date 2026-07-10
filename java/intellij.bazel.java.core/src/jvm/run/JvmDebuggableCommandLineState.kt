package org.jetbrains.bazel.jvm.run

import com.intellij.debugger.DefaultDebugEnvironment
import com.intellij.execution.Executor
import com.intellij.execution.JavaRunConfigurationExtensionManager
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.ConsoleView
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.run.config.BazelRunConfiguration

internal abstract class JvmDebuggableCommandLineState(
  environment: ExecutionEnvironment,
  private val port: Int,
  private val configuration: BazelRunConfiguration,
) : BazelCommandLineStateBase(environment) {
  fun createDebugEnvironment(environment: ExecutionEnvironment): DefaultDebugEnvironment {
    // create remote connection with client mode
    val remoteConnection =
      RemoteConnection(
        true,
        "localhost",
        port.toString(), // this is the port used
        false,
      )
    environment
      .runProfile
      .let { it as? RunConfigurationBase<*> }
      ?.let(::attachCoroutinesDebuggerConnection)
    return DefaultDebugEnvironment(
      environment,
      this,
      remoteConnection,
      true,
    )
  }

  override fun createConsole(executor: Executor): ConsoleView? {
    val console = super.createConsole(executor)
    return JavaRunConfigurationExtensionManager.instance.decorateExecutionConsole(
      configuration,
      runnerSettings,
      console ?: return null,
      executor,
    )
  }
}
