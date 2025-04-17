package org.jetbrains.bazel.jvm.run

import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.taskEvents.OriginId

abstract class JvmDebuggableCommandLineState(
  environment: ExecutionEnvironment,
  originId: OriginId,
  debugPort: Int,
) : BazelCommandLineStateBase(environment, originId) {
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
