package org.jetbrains.bazel.sync_new.codec

import java.nio.ByteBuffer
import java.nio.charset.Charset

interface CodecBuffer {
  val writable: Boolean
  val readable: Boolean
  val position: Int
  val size: Int

  fun reserve(size: Int)

  fun writeVarInt(value: Int)
  fun writeVarLong(value: Long)
  fun writeInt32(value: Int)
  fun writeInt64(value: Long)
  fun writeBytes(bytes: ByteArray)
  fun writeBuffer(buffer: ByteBuffer)

  fun readVarInt(): Int
  fun readVarLong(): Long
  fun readInt32(): Int
  fun readInt64(): Long
  fun readBytes(bytes: ByteArray)
  fun readBuffer(size: Int): ByteBuffer
}

fun CodecBuffer.writeString(value: String, charset: Charset = Charsets.UTF_8) {
  val bytes = value.toByteArray(charset)
  writeVarInt(bytes.size)
  writeBytes(bytes)
}

fun CodecBuffer.readString(charset: Charset = Charsets.UTF_8): String {
  val len = readVarInt()
  val bytes = ByteArray(len)
  readBytes(bytes)
  return String(bytes, charset)
}
