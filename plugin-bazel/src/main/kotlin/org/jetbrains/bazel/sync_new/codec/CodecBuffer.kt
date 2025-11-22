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
  fun writeInt8(value: Byte)
  fun writeInt32(value: Int)
  fun writeInt64(value: Long)
  fun writeBytes(bytes: ByteArray)
  fun writeBuffer(buffer: ByteBuffer)

  fun readVarInt(): Int
  fun readVarLong(): Long
  fun readInt8(): Byte
  fun readInt32(): Int
  fun readInt64(): Long
  fun readBytes(bytes: ByteArray)
  fun readBuffer(size: Int): ByteBuffer
}

interface HasByteBuffer : CodecBuffer {
  val buffer: ByteBuffer
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

fun CodecBuffer.writeBoolean(value: Boolean) {
  writeInt8(if (value) 1 else 0)
}

fun CodecBuffer.readBoolean(): Boolean = readInt8() != 0.toByte()

interface WritableOnlyCodecBuffer : CodecBuffer {
  override val writable: Boolean
    get() = true
  override val readable: Boolean
    get() = false

  override fun readVarInt(): Int = error("not supported")
  override fun readVarLong(): Long = error("not supported")
  override fun readInt8(): Byte = error("not supported")
  override fun readInt32(): Int = error("not supported")
  override fun readInt64(): Long = error("not supported")
  override fun readBytes(bytes: ByteArray) = error("not supported")
  override fun readBuffer(size: Int): ByteBuffer = error("not supported")
}

interface ReadableOnlyCodecBuffer : CodecBuffer {
  override val writable: Boolean
    get() = false
  override val readable: Boolean
    get() = true

  override fun reserve(size: Int) = error("not supported")
  override fun writeVarInt(value: Int) = error("not supported")
  override fun writeVarLong(value: Long) = error("not supported")
  override fun writeInt8(value: Byte) = error("not supported")
  override fun writeInt32(value: Int) = error("not supported")
  override fun writeInt64(value: Long) = error("not supported")
  override fun writeBytes(bytes: ByteArray) = error("not supported")
  override fun writeBuffer(buffer: ByteBuffer) = error("not supported")
}
