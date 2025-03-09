package org.jetbrains.bazel.jvm.run

import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.bazel.run.BazelCommandLineStateBase
import org.jetbrains.bazel.taskEvents.OriginId

abstract class JvmDebuggableCommandLineState(environment: ExecutionEnvironment, originId: OriginId) :
  BazelCommandLineStateBase(environment, originId) {
  val remoteConnection: RemoteConnection = RemoteConnection(true, "localhost", "0", true)

  fun getConnectionPort(): Int = remoteConnection.debuggerAddress.toInt()
}
