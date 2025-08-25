package org.jetbrains.bazel.debug.utils

import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.DebugEvent
import com.google.devtools.build.lib.starlarkdebugging.StarlarkDebuggingProtos.DebugRequest
import org.jetbrains.kotlin.utils.addToStdlib.popLast
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket

class MockSocket : Socket() {
  private val requestBuffer = StreamBuffer()
  private val eventBuffer = StreamBuffer()
  private var closed = false

  override fun getInputStream(): InputStream = eventBuffer.inputStream

  override fun getOutputStream(): OutputStream = requestBuffer.outputStream

  override fun close() {
    closed = true
  }

  override fun isClosed(): Boolean = closed

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

  fun simulateException(ex: Throwable?) {
    requestBuffer.simulateException(ex)
    eventBuffer.simulateException(ex)
  }
}

private class StreamBuffer {
  // UBytes to be sure that everything is in range [0, 255], as per java.io.InputStream::read() documentation
  private val buffer = ArrayDeque<UByte>()
  private var exceptionToThrow: Throwable? = null // used for simulating errors in streams

  val inputStream: InputStream =
    object : InputStream() {
      // this function never blocks - if no bytes are available, an EOF is returned
      override fun read(): Int {
        exceptionToThrow?.let { throw it }
        return when (buffer.isEmpty()) {
          false -> buffer.popLast().toInt()
          true -> -1
        }
      }
    }

  val outputStream: OutputStream =
    object : OutputStream() {
      override fun write(b: Int) {
        exceptionToThrow?.let { throw it }
        buffer.addFirst(b.toUByte())
      }
    }

  fun clear() {
    buffer.clear()
  }

  fun simulateException(ex: Throwable?) {
    exceptionToThrow = ex
  }
}
