package org.jetbrains.bazel.sync_new.storage.util

import java.nio.ByteBuffer

@JvmInline
value class InlineByteBufferCompat(val buffer: ByteBuffer) : ByteBufferCompat {
  override fun get(): Byte = buffer.get()

  override fun put(value: Byte) {
    buffer.put(value)
  }
}
