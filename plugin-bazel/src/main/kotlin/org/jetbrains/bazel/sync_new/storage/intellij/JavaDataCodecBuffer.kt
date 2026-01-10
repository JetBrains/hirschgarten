package org.jetbrains.bazel.sync_new.storage.intellij

import org.jetbrains.bazel.sync_new.codec.CodecContext
import org.jetbrains.bazel.sync_new.codec.ReadableOnlyCodecBuffer
import org.jetbrains.bazel.sync_new.codec.WritableOnlyCodecBuffer
import org.jetbrains.bazel.sync_new.storage.util.ByteBufferCompat
import org.jetbrains.bazel.sync_new.storage.util.ByteBufferUtils
import java.io.DataInput
import java.io.DataOutput
import java.nio.ByteBuffer

data object JavaDataCodecContext : CodecContext

@JvmInline
internal value class DataOutputCodecBuffer(val output: DataOutput) : WritableOnlyCodecBuffer, ByteBufferCompat {
  override val position: Int
    get() = error("not supported")
  override val size: Int
    get() = Int.MAX_VALUE

  override fun reserve(size: Int) {
    /* noop */
  }

  override fun writeVarInt(value: Int) {
    ByteBufferUtils.writeVarInt(this, value)
  }

  override fun writeVarLong(value: Long) {
    ByteBufferUtils.writeVarLong(this, value)
  }

  override fun writeInt8(value: Byte) {
    this.put(value)
  }

  override fun writeInt32(value: Int) {
    ByteBufferUtils.writeVarInt(this, value)
  }

  override fun writeInt64(value: Long) {
    ByteBufferUtils.writeVarLong(this, value)
  }

  override fun writeBytes(bytes: ByteArray, offset: Int, length: Int) {
    output.write(bytes, offset, length)
  }

  override fun writeBuffer(buffer: ByteBuffer) {
    if (buffer.hasArray()) {
      output.write(buffer.array(), buffer.arrayOffset() + buffer.position(), buffer.remaining())
    }
    else {
      val bytes = ByteArray(buffer.remaining())
      buffer.get(bytes)
      output.write(bytes)
    }
  }

  override fun get(): Byte {
    error("not supported")
  }

  override fun put(value: Byte) {
    output.writeByte(value.toInt())
  }
}

@JvmInline
internal value class DataInputCodecBuffer(val input: DataInput) : ReadableOnlyCodecBuffer, ByteBufferCompat {
  override val position: Int
    get() = error("not supported")
  override val size: Int
    get() = Int.MAX_VALUE

  override fun readVarInt(): Int {
    return ByteBufferUtils.readVarInt(this)
  }

  override fun readVarLong(): Long {
    return ByteBufferUtils.readVarLong(this)
  }

  override fun readInt8(): Byte {
    return get()
  }

  override fun readInt32(): Int {
    return ByteBufferUtils.readInt(this)
  }

  override fun readInt64(): Long {
    return ByteBufferUtils.readLong(this)
  }

  override fun readBytes(bytes: ByteArray, offset: Int, length: Int) {
    input.readFully(bytes, offset, length)
  }

  override fun readBuffer(size: Int): ByteBuffer {
    val array = ByteArray(size)
    input.readFully(array)
    return ByteBuffer.wrap(array)
  }

  override fun get(): Byte {
    return input.readByte()
  }

  override fun put(value: Byte) {
    error("not supported")
  }
}
