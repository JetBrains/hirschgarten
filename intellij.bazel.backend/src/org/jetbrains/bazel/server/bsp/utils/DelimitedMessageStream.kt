package org.jetbrains.bazel.server.bsp.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jetbrains.annotations.ApiStatus
import java.io.InputStream
import kotlin.time.Duration

/**
 * Reads the bytes of one message. A varint size prefix comes first, and the message body follows it.
 *
 * Throws [IllegalStateException] if the size prefix or the body does not arrive.
 * The prefix and the body each get their own [timeout].
 */
@ApiStatus.Internal
suspend fun InputStream.awaitDelimitedMessageBytes(
  timeout: Duration,
  interval: Duration,
  dispatcher: CoroutineDispatcher = Dispatchers.IO,
  stopCondition: () -> Boolean,
): ByteArray? = withContext(dispatcher) {
  if (!awaitAvailableBytesUnless(interval, stopCondition)) return@withContext null
  val size = awaitMessageSize(timeout, interval)
  if (size < 0) error("Invalid message size: $size")
  awaitMessage(timeout, interval, size)
}

// caller must provide io dispatcher
@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun InputStream.awaitAvailableBytesUnless(
  interval: Duration,
  stopCondition: () -> Boolean,
): Boolean {
  while (available() <= 0) {
    if (stopCondition()) return false
    delay(interval)
  }
  return true
}

// caller must provide io dispatcher
@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun InputStream.awaitMessage(
  timeout: Duration,
  interval: Duration,
  size: Int,
): ByteArray {
  val messageArray = ByteArray(size)
  var length = 0
  withTimeoutOrNull(timeout) {
    while (length < size) {
      val readLength = readNBytes(messageArray, length, size - length)
      if (readLength == 0) {
        delay(interval)
      }
      else {
        length += readLength
      }
    }
  } ?: error("Incomplete message received, timed out waiting for the message body: Received $length out of $size bytes")
  return messageArray
}

// caller must provide io dispatcher
// Adapted from CodedInputStream.readRawVarint32
private suspend fun InputStream.awaitMessageSize(
  timeout: Duration,
  interval: Duration,
): Int {
  val size = withTimeoutOrNull(timeout) {
    val firstByte = awaitByte(interval)
    if (firstByte.and(0x80) == 0) return@withTimeoutOrNull firstByte

    var result = firstByte.and(0x7F)
    for (offset in 7 until 32 step 7) {
      val byte = awaitByte(interval)
      result = result.or(byte.and(0x7F).shl(offset))
      if (byte.and(0x80) == 0) return@withTimeoutOrNull result
    }

    // A varint holds seven bits per byte, so a 64 bit value needs ten bytes. The code read five bytes above.
    // The bits of the last five bytes sit above bit 31, so they cannot change an Int. Read them to reach the body.
    repeat(5) {
      if (awaitByte(interval).and(0x80) == 0) return@withTimeoutOrNull result
    }
    error("Invalid message size format")
  }
  return size ?: error("Incomplete message received, timed out waiting for the message size to be written")
}

// caller must provide io dispatcher
@Suppress("BlockingMethodInNonBlockingContext")
private suspend fun InputStream.awaitByte(interval: Duration): Int {
  while (true) {
    val byte = read()
    if (byte == -1) {
      delay(interval)
    }
    else {
      return byte
    }
  }
}
