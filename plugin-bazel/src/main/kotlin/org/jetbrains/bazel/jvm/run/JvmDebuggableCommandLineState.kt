package org.jetbrains.bazel.jvm.run

import com.intellij.debugger.DefaultDebugEnvironment
import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bsp.protocol.DebugType

// Longer timeout to account for Bazel build
private const val DEBUGGER_ATTACH_TIMEOUT: Long = 60 * 60 * 1000

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

    return DefaultDebugEnvironment(
      environment,
      this,
      remoteConnection,
      DEBUGGER_ATTACH_TIMEOUT,
    )
  }

  val debugType: DebugType
    get() = DebugType.JDWP(port)
}
