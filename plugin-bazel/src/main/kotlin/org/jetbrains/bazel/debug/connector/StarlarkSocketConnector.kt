package org.jetbrains.bazel.debug.connector

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.DebugEvent
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.DebugRequest
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException
import kotlin.math.ceil

class StarlarkSocketConnector private constructor(
  private val socket: Socket,
  private val onSocketBreak: () -> Unit,
) {
  private val ins: InputStream = socket.getInputStream()
  private val outs: OutputStream = socket.getOutputStream()

  fun write(request: DebugRequest) {
    try {
      write(request.toByteArray())
    } catch (se: SocketException) {
      this.onSocketBreak()
    }
  }

  private fun write(bytes: ByteArray) {
    outs.writeUVarIntBytes(bytes.size)
    outs.write(bytes)
    outs.flush()
  }

  fun read(): DebugEvent? {
    val len = ins.readUVarInt()
    return when {
      len > 0 -> ins.readNBytes(len).let { DebugEvent.parseFrom(it) }
      len < 0 -> {
        this.onSocketBreak()
        null
      }
      else -> null
    }
  }

  /** This function needs to be safe to call multiple times */
  fun close() {
    if (!socket.isClosed) socket.close()
  }

  fun isClosed(): Boolean = socket.isClosed

  companion object {
    fun connectTo(port: Int, onSocketBreak: () -> Unit): StarlarkSocketConnector {
      return StarlarkSocketConnector(Socket(HOST, port), onSocketBreak)
    }
  }
}

private const val HOST = "localhost"
//private const val PORT = 7300
private val MAX_INT_UVARINT_SIZE = ceil(Int.SIZE_BITS / 7.0).toInt()

private fun OutputStream.writeUVarIntBytes(number: Int) {
  var x = number
  while (x >= 0x80) {
    write((x or 0x80) and 0xff)
    x = x shr 7
  }
  write(x)
}

private fun InputStream.readUVarInt(): Int {
  var x = 0
  var bytesRead = 0
  var byte: Int
  while (bytesRead < MAX_INT_UVARINT_SIZE) {
    byte = try {
      read()
    } catch (_: SocketException) {
      -1
    }
    if (byte == -1) {
      return -1  // stream died
    }
    x = x or ((byte and 0x7f) shl bytesRead * 7)
    if (byte < 0x80) return x
    bytesRead++
  }
  throw ArithmeticException("UVarInt parsing failed (no ending byte found)")
}
