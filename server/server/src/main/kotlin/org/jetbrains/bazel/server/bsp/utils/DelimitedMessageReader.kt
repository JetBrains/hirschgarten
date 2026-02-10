package org.jetbrains.bazel.server.bsp.utils

import com.google.protobuf.InvalidProtocolBufferException
import com.google.protobuf.Message
import com.google.protobuf.Parser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import java.io.InputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Handles reading messages from a delimited input source when the source is being simultaneously written to (by another process for example)
 */
class DelimitedMessageReader<MessageType : Message>(
  private val inputStream: InputStream,
  private val parser: Parser<MessageType>,
  private val timeout: Duration = DefaultTimeout
) {
  /**
   * Returns the next message if available.
   * If there is no input available, the function returns instantly.
   * If there is data, the function will block until the full message is received
   * InvalidProtocolBufferException will be thrown if a timeout occurs waiting for the full message to be received
   */
  suspend fun nextMessage(): MessageType? =
    withContext(Dispatchers.IO) {
      if (inputStream.available() <= 0) {
        return@withContext null
      }
      val size =
        readRawVarint32().also { size ->
          try {
            withTimeout(timeout) {
              while (isActive) {
                if (inputStream.available() >= size) {
                  break
                }
                yield()
              }
            }
          } catch (e: TimeoutCancellationException) {
            throw InvalidProtocolBufferException(
              "Incomplete message received, timed out waiting for remaining message to be written: Received ${inputStream.available()} out of $size bytes",
              e,
            )
          }
        }
      val messageArray =
        ByteArray(size).also {
          inputStream.read(it)
        }
      return@withContext parser.parsePartialFrom(messageArray)
    }

  // Adapted from CodedInputStream.readRawVarint32
  private suspend fun readRawVarint32(): Int {
    try {
      val firstByte = readByte()
      if (firstByte.and(0x80) == 0) {
        return firstByte
      }

      var result = firstByte.and(0x7F)
      for (offset in 7 until 32 step 7) {
        val byte = readByte()
        result = result.or(byte.and(0x7F).shl(offset))
        if (byte.and(0x80) == 0) {
          return result
        }
      }

      // Keep reading, but not appending to the result
      (32 until 64 step 7).forEach {
        val byte = readByte()
        if (byte.and(0x80) == 0) {
          return result
        }
      }

      throw IllegalStateException("Invalid message size format")
    } catch (e: TimeoutCancellationException) {
      throw InvalidProtocolBufferException("Incomplete message received, timed out waiting for message size to be written", e)
    }
  }

  private suspend fun readByte(): Int =
    withTimeout(1.seconds) {
      while (isActive) {
        val byte = inputStream.read()
        if (byte == -1) {
          yield()
        } else {
          return@withTimeout byte
        }
      }
      // Can't ever get here, but the compiler is being tricky
      throw IllegalStateException()
    }

  companion object {
    val DefaultTimeout = 30.seconds
  }
}
