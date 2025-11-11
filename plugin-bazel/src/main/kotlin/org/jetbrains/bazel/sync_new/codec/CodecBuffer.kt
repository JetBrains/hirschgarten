package org.jetbrains.bazel.sync_new.codec

import java.nio.ByteBuffer

interface CodecBuffer {
  val writable: Boolean
  val readable: Boolean
  val position: Int
  val size: Int

  fun reserve(size: Int)

  fun writeInt32(value: Int)
  fun writeInt64(value: Long)
  fun writeBytes(bytes: ByteArray)
  fun writeBuffer(buffer: ByteBuffer)

  fun readInt32(): Int
  fun readInt64(): Long
  fun readBytes(bytes: ByteArray)
  fun readBuffer(size: Int): ByteBuffer
}
