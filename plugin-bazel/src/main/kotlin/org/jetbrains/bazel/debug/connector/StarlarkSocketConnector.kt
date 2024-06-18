package org.jetbrains.bazel.debug.connector

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.DebugEvent
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.DebugRequest
import org.jetbrains.annotations.TestOnly
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException

class StarlarkSocketConnector private constructor(
  private val socket: Socket,
  private val onSocketBreak: () -> Unit,
) {
  private val ins: InputStream = socket.getInputStream()
  private val outs: OutputStream = socket.getOutputStream()

  fun write(request: DebugRequest) {
    try {
      request.writeDelimitedTo(outs)
    } catch (_: SocketException) {
      this.onSocketBreak()
    }
  }

  fun read(): DebugEvent? {
    val ret = try {
      DebugEvent.parseDelimitedFrom(ins)
    } catch (_: IOException) {
      null
    }
    if (ret == null) this.onSocketBreak()
    return ret
  }

  /** This function needs to be safe to call multiple times */
  fun close() {
    if (!socket.isClosed) socket.close()
  }

  fun isClosed(): Boolean = socket.isClosed

  companion object {
    fun tryConnectTo(
      port: Int,
      onSocketBreak: () -> Unit,
      attempts: Int,
      intervalMillis: Long,
    ): StarlarkSocketConnector =
      StarlarkSocketConnector(
        socket = establishSocket(port, attempts, intervalMillis),
        onSocketBreak = onSocketBreak,
      )

    private fun establishSocket(
      port: Int,
      attempts: Int,
      intervalMillis: Long,
    ): Socket {
      repeat(attempts - 1) {
        try {
          return Socket(HOST, port)
        } catch (_: IOException) {
          // ignore IOExceptions on all but the last attempt; other exceptions are thrown normally
          Thread.sleep(intervalMillis)
        }
      }
      // this is the last attempt, throw every exception
      return Socket(HOST, port)
    }

    @TestOnly
    fun simpleSocketConnection(socket: Socket): StarlarkSocketConnector =
      StarlarkSocketConnector(
        socket = socket,
        onSocketBreak = {}
      )
  }
}

private const val HOST = "localhost"
