package org.jetbrains.plugins.bsp.jvm

import com.intellij.execution.configurations.RemoteConnection

interface JvmDebuggableCommandLineState {
  val remoteConnection: RemoteConnection
    get() = RemoteConnection(true, "localhost", "0", true)

  val portForDebug: Int?
    get() = remoteConnection.debuggerAddress?.toInt()
}
