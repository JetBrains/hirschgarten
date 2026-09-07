package org.jetbrains.bazel.server.bsp.utils

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.jetbrains.bazel.commons.constants.Constants
import org.junit.jupiter.api.Test
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class DelimitedMessageStreamTest {
  @Test
  fun `should read every message of a delimited stream`() = runTest {
    val events = (0..3).map { startedEvent(uuid = it.toString()) }
    val inputStream = ByteArrayInputStream(delimitedBytes(events))
    val result = events.map { inputStream.awaitDelimitedMessageBytes(TestTimeout, TestInterval, currentDispatcher) { false } }
    result.map { parseEvent(it) } shouldContainExactly events
  }

  @Test
  fun `should read a message with a size above 2^8`() = runTest {
    val message = startedEvent(uuid = (0L..2.0.pow(8.0).toInt()).joinToString("") { "a" })
    val inputStream = ByteArrayInputStream(delimitedBytes(message))

    val bytes = inputStream.awaitDelimitedMessageBytes(TestTimeout, TestInterval, currentDispatcher) { false }

    parseEvent(bytes) shouldBe message
  }

  @Test
  fun `should read a size prefix that uses five bytes`() = runTest {
    val inputStream = ByteArrayInputStream(paddedSizePrefix(length = 5) + BodyByte)

    val bytes = inputStream.awaitDelimitedMessageBytes(TestTimeout, TestInterval, currentDispatcher) { false }

    bytes.shouldNotBeNull().toList() shouldContainExactly listOf(BodyByte)
  }

  @Test
  fun `should read a size prefix that uses ten bytes`() = runTest {
    val inputStream = ByteArrayInputStream(paddedSizePrefix(length = 10) + BodyByte)

    val bytes = inputStream.awaitDelimitedMessageBytes(TestTimeout, TestInterval, currentDispatcher) { false }

    bytes.shouldNotBeNull().toList() shouldContainExactly listOf(BodyByte)
  }

  @Test
  fun `should fail on a size prefix longer than ten bytes`() = runTest {
    val inputStream = ByteArrayInputStream(ByteArray(10) { PadByte })

    val error =
      shouldThrow<IllegalStateException> {
        inputStream.awaitDelimitedMessageBytes(TestTimeout, TestInterval, currentDispatcher) { false }
      }

    error.message shouldBe "Invalid message size format"
  }

  @Test
  fun `should read a message that arrives one byte at a time`() = runTest {
    val message = startedEvent(uuid = "uuid")
    val inputStream = PacedInputStream(delimitedBytes(message), period = TestInterval) { currentTime }

    val bytes = inputStream.awaitDelimitedMessageBytes(TestTimeout, TestInterval, currentDispatcher) { false }

    parseEvent(bytes) shouldBe message
  }

  @Test
  fun `should wait for a message that arrives late`() = runTest {
    val message = startedEvent(uuid = "uuid")
    val inputStream = GrowingInputStream(delimitedBytes(message), head = 0, refusal = 1200.milliseconds) { currentTime }
    val bytes = inputStream.awaitDelimitedMessageBytes(2.seconds, TestInterval, currentDispatcher) { false }
    parseEvent(bytes) shouldBe message
  }

  @Test
  fun `should wait for a body that arrives late`() = runTest {
    val message = startedEvent(uuid = "uuid")
    val inputStream = GrowingInputStream(delimitedBytes(message), head = 3, refusal = 100.milliseconds) { currentTime }
    val bytes = inputStream.awaitDelimitedMessageBytes(TestTimeout, TestInterval, currentDispatcher) { false }
    parseEvent(bytes) shouldBe message
  }

  @Test
  fun `should read the whole body after a short read`() = runTest {
    val messages = listOf(startedEvent(uuid = "a".repeat(300)), startedEvent(uuid = "b".repeat(300)))
    val inputStream = EstimateAvailableInputStream(delimitedBytes(messages), chunk = 1)
    val result = messages.map { inputStream.awaitDelimitedMessageBytes(10.seconds, TestInterval, currentDispatcher) { false } }
    result.map { parseEvent(it) } shouldContainExactly messages
  }

  @Test
  fun `should read every message through a buffered stream that gives short reads`() = runTest {
    val messages = (0..3).map { startedEvent(uuid = "$it".repeat(200)) }
    val source = EstimateAvailableInputStream(delimitedBytes(messages), chunk = 7)
    val inputStream = BufferedInputStream(source)

    val result = messages.map { inputStream.awaitDelimitedMessageBytes(TestTimeout, TestInterval, currentDispatcher) { false } }

    result.map { parseEvent(it) } shouldContainExactly messages
    source.available() shouldBe 0
  }

  @Test
  fun `should fail on a partial message`() = runTest {
    val partial = delimitedBytes(startedEvent(uuid = "uuid")).sliceArray(0..2)
    val inputStream = ByteArrayInputStream(partial)
    val error =
      shouldThrow<IllegalStateException> {
        inputStream.awaitDelimitedMessageBytes(TestTimeout, TestInterval, currentDispatcher) { false }
      }
    error.message shouldContain "timed out waiting for the message body"
  }

  @Test
  fun `should fail on a negative size`() = runTest {
    val inputStream = ByteArrayInputStream(byteArrayOf(-1, -1, -1, -1, 0x0F))
    val error =
      shouldThrow<IllegalStateException> {
        inputStream.awaitDelimitedMessageBytes(TestTimeout, TestInterval, currentDispatcher) { false }
      }
    error.message shouldBe "Invalid message size: -1"
  }

  @Test
  fun `should return no bytes when the stream is drained and the stop condition holds`() = runTest {
    val inputStream = ByteArrayInputStream(ByteArray(0))

    inputStream.awaitDelimitedMessageBytes(TestTimeout, TestInterval, currentDispatcher) { true } shouldBe null
  }

  @Test
  fun `should read a message that is available even when the stop condition holds`() = runTest {
    val message = startedEvent(uuid = "uuid")
    val inputStream = ByteArrayInputStream(delimitedBytes(message))

    val bytes = inputStream.awaitDelimitedMessageBytes(TestTimeout, TestInterval, currentDispatcher) { true }

    parseEvent(bytes) shouldBe message
  }
}

private val TestInterval = 10.milliseconds
private val TestTimeout = 1.seconds

@OptIn(ExperimentalStdlibApi::class)
private val TestScope.currentDispatcher: CoroutineDispatcher
  get() = this.coroutineContext[CoroutineDispatcher]!!

private const val SizeOneByte: Byte = -0x7F

private const val PadByte: Byte = -0x80

private const val BodyByte: Byte = 0x2A

private fun paddedSizePrefix(length: Int): ByteArray = byteArrayOf(SizeOneByte) + ByteArray(length - 2) { PadByte } + byteArrayOf(0)

private fun parseEvent(bytes: ByteArray?): BuildEventStreamProtos.BuildEvent =
  BuildEventStreamProtos.BuildEvent.parser().parsePartialFrom(bytes.shouldNotBeNull())

private fun startedEvent(uuid: String): BuildEventStreamProtos.BuildEvent =
  BuildEventStreamProtos.BuildEvent
    .newBuilder()
    .apply {
      started =
        startedBuilder
          .apply {
            this.uuid = uuid
            command = Constants.BAZEL_BUILD_COMMAND
          }.build()
    }.build()

private fun delimitedBytes(message: BuildEventStreamProtos.BuildEvent): ByteArray = delimitedBytes(listOf(message))

private fun delimitedBytes(messages: List<BuildEventStreamProtos.BuildEvent>): ByteArray =
  ByteArrayOutputStream().let { baos ->
    messages.forEach { it.writeDelimitedTo(baos) }
    baos.toByteArray()
  }

private class PacedInputStream(
  private val bytes: ByteArray,
  private val period: Duration,
  private val now: () -> Long,
) : InputStream() {
  private val start = now()
  private var position = 0

  private fun limit(): Int = minOf(bytes.size, 1 + ((now() - start) / period.inWholeMilliseconds).toInt())

  override fun available(): Int = limit() - position

  override fun read(): Int = if (position < limit()) bytes[position++].toInt().and(0xFF) else -1
}

/**
 * A stream that holds only the first [head] bytes until [refusal] passes, and holds every byte after that.
 */
private class GrowingInputStream(
  private val bytes: ByteArray,
  private val head: Int,
  refusal: Duration,
  private val now: () -> Long,
) : InputStream() {
  private val deadline = now() + refusal.inWholeMilliseconds
  private var position = 0

  private fun limit(): Int = if (now() < deadline) head else bytes.size

  override fun available(): Int = limit() - position

  override fun read(): Int = if (position < limit()) bytes[position++].toInt().and(0xFF) else -1
}

/**
 * A stream that reports every remaining byte through [available], but gives at most [chunk] bytes per array read.
 */
private class EstimateAvailableInputStream(
  private val bytes: ByteArray,
  private val chunk: Int,
) : InputStream() {
  private var position = 0
  private var refuse = false

  override fun available(): Int = bytes.size - position

  override fun read(): Int = if (position < bytes.size) bytes[position++].toInt().and(0xFF) else -1

  override fun read(
    b: ByteArray,
    off: Int,
    len: Int,
  ): Int {
    if (position >= bytes.size) {
      return -1
    }
    refuse = !refuse
    if (refuse) {
      return -1
    }
    val count = minOf(chunk, len, bytes.size - position)
    bytes.copyInto(b, off, position, position + count)
    position += count
    return count
  }
}
