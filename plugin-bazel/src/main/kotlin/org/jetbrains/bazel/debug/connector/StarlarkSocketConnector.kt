package org.jetbrains.bazel.debug.connector

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.DebugEvent
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.DebugRequest
import org.jetbrains.annotations.TestOnly
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class StarlarkSocketConnector private constructor(
  private val socket: Socket,
  private val onSocketBreak: () -> Unit,
) {
  private val ins: InputStream = socket.getInputStream()
  private val outs: OutputStream = socket.getOutputStream()

  @Throws(IOException::class)
  fun write(request: DebugRequest) {
    request.writeDelimitedTo(outs)
  }

  @Throws(IOException::class)
  fun read(): DebugEvent {
    if (socket.isClosed) {
      throw IOException("Socket closed")
    }
    return DebugEvent.parseDelimitedFrom(ins)  // will be null when EOF is reached
      .also { if (it == null) throw IOException("Socket stream is closed") }
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
