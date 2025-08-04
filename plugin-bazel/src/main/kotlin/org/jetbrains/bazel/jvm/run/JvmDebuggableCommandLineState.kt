package org.jetbrains.bazel.jvm.run

import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.run.BazelCommandLineStateBase

abstract class JvmDebuggableCommandLineState(environment: ExecutionEnvironment, debugPort: Int) : BazelCommandLineStateBase(environment) {
  // create remote connection with client mode
  val remoteConnection: RemoteConnection =
    RemoteConnection(
      true,
      "localhost",
      debugPort.toString(), // this is the port used
      false,
    )

  fun getConnectionPort(): Int = remoteConnection.debuggerAddress.toInt()
}
