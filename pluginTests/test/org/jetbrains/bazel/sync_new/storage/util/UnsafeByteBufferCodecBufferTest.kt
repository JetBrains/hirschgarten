package org.jetbrains.bazel.sync_new.storage.util

import org.jetbrains.bazel.sync_new.codec.CodecBufferTest
import org.jetbrains.bazel.sync_new.codec.CodecBufferTestCase
import org.jetbrains.bazel.test.framework.CartesianProduct
import org.junit.jupiter.params.provider.Arguments

internal class UnsafeByteBufferCodecBufferTest : CodecBufferTest() {
  companion object {

    @JvmStatic
    fun testCases() = listOf(
      object : CodecBufferTestCase<UnsafeByteBufferCodecBuffer>(name = "heap") {
        override fun createWritableBuffer(): UnsafeByteBufferCodecBuffer {
          return UnsafeByteBufferCodecBuffer.allocateHeap()
        }

        override fun createReadableBuffer(input: UnsafeByteBufferCodecBuffer): UnsafeByteBufferCodecBuffer {
          input.buffer.flip()
          return input
        }
      },
      object : CodecBufferTestCase<UnsafeByteBufferCodecBuffer>(name = "offheap") {
        override fun createWritableBuffer(): UnsafeByteBufferCodecBuffer {
          return UnsafeByteBufferCodecBuffer.allocateUnsafe()
        }

        override fun createReadableBuffer(input: UnsafeByteBufferCodecBuffer): UnsafeByteBufferCodecBuffer {
          input.buffer.flip()
          return input
        }
      },
    )

    @JvmStatic
    fun testCasesWithBufferSizes() = CartesianProduct.make2d(testCases(), bufferSizes())
      .map { (c1, c2) -> Arguments.of(c1, c2) }
  }

}
