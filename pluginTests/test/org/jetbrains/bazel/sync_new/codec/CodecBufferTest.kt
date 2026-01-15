package org.jetbrains.bazel.sync_new.codec

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.nio.ByteBuffer

internal abstract class CodecBufferTest {
  companion object {
    fun bufferSizes() = listOf(1, 1024, 8192, 1024 * 1024, 64 * 1024 * 1024)
  }

  fun <V : CodecBuffer> testRW(testCase: CodecBufferTestCase<V>, write: V.() -> Unit, read: V.() -> Unit) {
    val writable = testCase.createWritableBuffer()
    write(writable)
    val readable = testCase.createReadableBuffer(writable)
    read(readable)
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testCases")
  fun <V : CodecBuffer> `test var int`(testCase: CodecBufferTestCase<V>) {
    val values = listOf(0, 10, 127, 128, 255, 16383, 16384, Int.MAX_VALUE)
    testRW(
      testCase = testCase,
      write = {
        for (n in values) {
          writeVarInt(n)
        }
      },
      read = {
        for (n in values) {
          val read = readVarInt()
          n.shouldBe(read)
        }
      },
    )
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testCases")
  fun <V : CodecBuffer> `test var long`(testCase: CodecBufferTestCase<V>) {
    val values = listOf(0L, 10L, 127L, 128L, 255L, 16383L, 16384L, Long.MAX_VALUE, Long.MIN_VALUE, -1L)
    testRW(
      testCase = testCase,
      write = {
        for (n in values) {
          writeVarLong(n)
        }
      },
      read = {
        for (n in values) {
          val read = readVarLong()
          n.shouldBe(read)
        }
      },
    )
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testCases")
  fun <V : CodecBuffer> `test int32`(testCase: CodecBufferTestCase<V>) {
    val values = listOf(0, 10, 127, 128, 255, 16383, 16384, Int.MAX_VALUE, Int.MIN_VALUE, -1)
    testRW(
      testCase = testCase,
      write = {
        for (n in values) {
          writeInt32(n)
        }
      },
      read = {
        for (n in values) {
          val read = readInt32()
          n.shouldBe(read)
        }
      },
    )
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testCases")
  fun <V : CodecBuffer> `test int64`(testCase: CodecBufferTestCase<V>) {
    val values = listOf(0L, 10L, 127L, 128L, 255L, 16383L, 16384L, Long.MAX_VALUE, Long.MIN_VALUE, -1L)
    testRW(
      testCase = testCase,
      write = {
        for (n in values) {
          writeInt64(n)
        }
      },
      read = {
        for (n in values) {
          val read = readInt64()
          n.shouldBe(read)
        }
      },
    )
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testCases")
  fun <V : CodecBuffer> `test int8`(testCase: CodecBufferTestCase<V>) {
    val values = listOf(0.toByte(), 10.toByte(), 127.toByte(), (-128).toByte(), (-1).toByte())
    testRW(
      testCase = testCase,
      write = {
        for (n in values) {
          writeInt8(n)
        }
      },
      read = {
        for (n in values) {
          val read = readInt8()
          n.shouldBe(read)
        }
      },
    )
  }

  @ParameterizedTest(name = "{0} - {1}")
  @MethodSource("testCasesWithBufferSizes")
  fun <V : CodecBuffer> `test bytes rw`(testCase: CodecBufferTestCase<V>, size: Int) {
    val expected = ByteArray(size) { (it % 256).toByte() }
    testRW(
      testCase = testCase,
      write = {
        writeBytes(expected)
      },
      read = {
        val result = ByteArray(size)
        readBytes(result)
        result.shouldBe(expected)
      },
    )
  }

  @ParameterizedTest(name = "{0} - {1}")
  @MethodSource("testCasesWithBufferSizes")
  fun <V : CodecBuffer> `test byte buffer rw`(testCase: CodecBufferTestCase<V>, size: Int) {
    val expected = ByteBuffer.allocate(size)
    for (i in 0 until size) {
      expected.put((i % 256).toByte())
    }
    expected.flip()
    testRW(
      testCase = testCase,
      write = {
        writeBuffer(expected)
      },
      read = {
        val result = readBuffer(size)

        expected.rewind()
        result.rewind()

        for (n in 0 until size) {
          expected.get().shouldBe(result.get())
        }
      },
    )
  }


}

internal abstract class CodecBufferTestCase<V : CodecBuffer>(
  val name: String,
) {
  abstract fun createWritableBuffer(): V
  abstract fun createReadableBuffer(input: V): V

  override fun toString(): String = name
}
