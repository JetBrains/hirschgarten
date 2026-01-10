package org.jetbrains.bazel.sync_new.storage

import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync_new.codec.LabelCodec
import org.jetbrains.bazel.sync_new.storage.hash.hash
import org.jetbrains.bazel.sync_new.storage.util.UnsafeByteBufferCodecBuffer
import org.jetbrains.bazel.sync_new.storage.util.UnsafeCodecContext
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

internal class HashingTest {

  @Test
  fun `test hashing`() {
    val label1 = Label.parse("@//kotlin_stdlib:kt_lib")
    val hash1 = hash(label1)

    val buffer = UnsafeByteBufferCodecBuffer.allocateHeap()
    LabelCodec.encode(UnsafeCodecContext, buffer, label1)

    val label2 = LabelCodec.decode(UnsafeCodecContext, UnsafeByteBufferCodecBuffer(ByteBuffer.wrap(buffer.array)))
    val hash2 = hash(label2)

    label1.shouldBe(label2)
    hash1.shouldBe(hash2)
  }
}
