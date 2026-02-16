package org.jetbrains.bazel.server.bsp.utils

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.jetbrains.bazel.commons.constants.Constants
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

class DelimitedMessageReaderTest {
  @Test
  fun `test delimited messages are read`() =
    runTest {
      val buildStartedEvents =
        buildList {
          (0..3).forEach { i ->
            BuildEventStreamProtos.BuildEvent
              .newBuilder()
              .apply {
                started =
                  startedBuilder
                    .apply {
                      uuid = i.toString()
                      command = Constants.BAZEL_BUILD_COMMAND
                    }.build()
              }.build()
              .also {
                add(it)
              }
          }
        }
      val ba =
        ByteArrayOutputStream().let { baos ->
          buildStartedEvents.forEach { buildEvent ->
            buildEvent.writeDelimitedTo(baos)
          }
          baos.toByteArray()
        }

      val reader = DelimitedMessageReader(ByteArrayInputStream(ba), BuildEventStreamProtos.BuildEvent.parser())

      val result = listOf(reader.nextMessage(), reader.nextMessage(), reader.nextMessage(), reader.nextMessage())

      Assertions.assertEquals(buildStartedEvents, result)
      Assertions.assertNull(reader.nextMessage())
    }

  @Test
  fun `test partial message handling`() =
    runTest {
      val message =
        BuildEventStreamProtos.BuildEvent
          .newBuilder()
          .apply {
            started =
              startedBuilder
                .apply {
                  uuid = "uuid"
                  command = Constants.BAZEL_BUILD_COMMAND
                }.build()
          }.build()

      val ba =
        ByteArrayOutputStream().let {
          message.writeDelimitedTo(it)
          it.toByteArray()
        }

      val partial = ba.sliceArray(0..2)

      val reader =
        DelimitedMessageReader(
          ByteArrayInputStream(partial),
          BuildEventStreamProtos.BuildEvent.parser(),
          timeout = 1.seconds
        )

      try {
        reader.nextMessage()
        fail("Exception expected when a partial message was sent")
      } catch (_: InvalidProtocolBufferException) {
        // expected
      }
    }

  @Test
  fun `test message handling with stream of bytes`() =
    runTest {
      val message =
        BuildEventStreamProtos.BuildEvent
          .newBuilder()
          .apply {
            started =
              startedBuilder
                .apply {
                  uuid = "uuid"
                  command = Constants.BAZEL_BUILD_COMMAND
                }.build()
          }.build()

      val ba =
        ByteArrayOutputStream().let {
          message.writeDelimitedTo(it)
          it.toByteArray()
        }

      val outputStream = PipedOutputStream()
      val inputStream = PipedInputStream(outputStream)

      outputStream.write(ba.first().toInt())
      launch {
        for (i in 1 until ba.size) {
          outputStream.write(ba[i].toInt())
          delay(1)
        }
      }

      val reader = DelimitedMessageReader(inputStream, BuildEventStreamProtos.BuildEvent.parser())
      val result = reader.nextMessage()

      Assertions.assertEquals(message, result)
    }

  @Test
  fun `test message with length greater then 2^8`() =
    runTest {
      val message =
        BuildEventStreamProtos.BuildEvent
          .newBuilder()
          .apply {
            started =
              startedBuilder
                .apply {
                  uuid = (0L..2.0.pow(8.0).toInt()).joinToString("") { "a" }
                  command = Constants.BAZEL_BUILD_COMMAND
                }.build()
          }.build()

      val ba =
        ByteArrayOutputStream().let {
          message.writeDelimitedTo(it)
          it.toByteArray()
        }

      val reader = DelimitedMessageReader(ByteArrayInputStream(ba), BuildEventStreamProtos.BuildEvent.parser())
      val result = reader.nextMessage()

      Assertions.assertEquals(message, result)
    }
}
