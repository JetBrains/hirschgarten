package org.jetbrains.bazel.sync_new.storage.util

import io.kotest.matchers.shouldBe
import org.junit.Test
import java.nio.ByteBuffer

internal class ByteBufferUtilsTest {

  @Test
  fun `test var int serialization and deserialization`() {
    val buffer = ByteBuffer.allocate(32)
    val testValues = listOf(0, 10, 127, 128, 255, 16383, 16384, Int.MAX_VALUE)

    for (value in testValues) {
      ByteBufferUtils.writeVarInt(InlineByteBufferCompat(buffer), value)
    }

    buffer.flip()

    for (expectedValue in testValues) {
      val actualValue = ByteBufferUtils.readVarInt(InlineByteBufferCompat(buffer))
      actualValue.shouldBe(expectedValue)
    }
  }

  @Test
  fun `test var long serialization and deserialization`() {
    val buffer = ByteBuffer.allocate(64)
    val testValues = listOf(0L, 10L, 127L, 128L, 255L, 16383L, 16384L, Long.MAX_VALUE)

    for (value in testValues) {
      ByteBufferUtils.writeVarLong(InlineByteBufferCompat(buffer), value)
    }
    
    buffer.flip()

    for (expectedValue in testValues) {
      val actualValue = ByteBufferUtils.readVarLong(InlineByteBufferCompat(buffer))
      actualValue.shouldBe(expectedValue)
    }
  }

  @Test
  fun `test int serialization and deserialization`() {
    val buffer = ByteBuffer.allocate(64)
    val testValues = listOf(0, 10, 127, 128, 255, 16383, 16384, Int.MAX_VALUE, Int.MIN_VALUE, -1)

    for (value in testValues) {
      ByteBufferUtils.writeInt(InlineByteBufferCompat(buffer), value)
    }
    
    buffer.flip()

    for (expectedValue in testValues) {
      val actualValue = ByteBufferUtils.readInt(InlineByteBufferCompat(buffer))
      actualValue.shouldBe(expectedValue)
    }
  }

  @Test
  fun `test long serialization and deserialization`() {
    val buffer = ByteBuffer.allocate(80)
    val testValues = listOf(0L, 10L, 127L, 128L, 255L, 16383L, 16384L, Long.MAX_VALUE, Long.MIN_VALUE, -1L)

    for (value in testValues) {
      ByteBufferUtils.writeLong(InlineByteBufferCompat(buffer), value)
    }
    
    buffer.flip()

    for (expectedValue in testValues) {
      val actualValue = ByteBufferUtils.readLong(InlineByteBufferCompat(buffer))
      actualValue.shouldBe(expectedValue)
    }
  }
  
  
}
