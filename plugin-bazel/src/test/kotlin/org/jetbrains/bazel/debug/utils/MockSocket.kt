package org.jetbrains.bazel.debug.utils

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.DebugEvent
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.DebugRequest
import org.jetbrains.kotlin.backend.common.pop
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class MockSocket : Socket() {
  private val requestBuffer = StreamBuffer()
  private val eventBuffer = StreamBuffer()

  override fun getInputStream(): InputStream =
    eventBuffer.inputStream

  override fun getOutputStream(): OutputStream =
    requestBuffer.outputStream

  fun sendMockEvent(event: DebugEvent) {
    event.writeDelimitedTo(eventBuffer.outputStream)
  }

  fun readRequests(): List<DebugRequest> =
    generateSequence { DebugRequest.parseDelimitedFrom(requestBuffer.inputStream) }
      .toList()

  fun clearBuffers() {
    requestBuffer.clear()
    eventBuffer.clear()
  }
}

private class StreamBuffer {
  // UBytes to be sure that everything is in range [0, 255], as per java.io.InputStream::read() documentation
  private val buffer = ArrayDeque<UByte>()

  val inputStream: InputStream = object : InputStream() {
    override fun read(): Int =
      try {
        buffer.pop().toInt()
      } catch (_: IndexOutOfBoundsException) {
        -1
      }
  }

  val outputStream: OutputStream = object : OutputStream() {
    override fun write(b: Int) {
      buffer.addFirst(b.toUByte())
    }
  }

  fun clear() {
    buffer.clear()
  }
}
