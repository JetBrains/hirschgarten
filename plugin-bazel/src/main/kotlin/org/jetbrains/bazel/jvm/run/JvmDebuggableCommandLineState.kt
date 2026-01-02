package org.jetbrains.bazel.jvm.run

import com.intellij.debugger.DefaultDebugEnvironment
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.run.BazelCommandLineStateBase

// Longer timeout to account for Bazel build
private const val DEBUGGER_ATTACH_TIMEOUT: Long = 3 * 60 * 1000

abstract class JvmDebuggableCommandLineState(environment: ExecutionEnvironment, private val port: Int) :
  BazelCommandLineStateBase(environment) {
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
      DEBUGGER_ATTACH_TIMEOUT,
    )
  }
}
