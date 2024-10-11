package org.jetbrains.plugins.bsp.jvm

import com.intellij.execution.configurations.RemoteConnection
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.plugins.bsp.run.BspCommandLineStateBase
import org.jetbrains.plugins.bsp.taskEvents.OriginId

abstract class JvmDebuggableCommandLineState(environment: ExecutionEnvironment, originId: OriginId) :
  BspCommandLineStateBase(environment, originId) {
  val remoteConnection: RemoteConnection = RemoteConnection(true, "localhost", "0", true)

  fun getConnectionPort(): Int = remoteConnection.debuggerAddress.toInt()
}
