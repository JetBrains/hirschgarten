package org.jetbrains.bazel.sync_new.storage.intellij

import org.jetbrains.bazel.sync_new.codec.CodecContext
import org.jetbrains.bazel.sync_new.codec.ReadableOnlyCodecBuffer
import org.jetbrains.bazel.sync_new.codec.WritableOnlyCodecBuffer
import java.io.DataInput
import java.io.DataOutput
import java.nio.ByteBuffer

data object JavaDataCodecContext : CodecContext

@JvmInline
internal value class DataOutputCodecBuffer(val output: DataOutput) : WritableOnlyCodecBuffer {
  override val position: Int
    get() = error("not supported")
  override val size: Int
    get() = Int.MAX_VALUE

  override fun reserve(size: Int) {
    /* noop */
  }

  override fun writeVarInt(value: Int) {
    DataStreamUtils.writeVarInt32(this.output, value)
  }

  override fun writeVarLong(value: Long) {
    DataStreamUtils.writeVarInt64(this.output, value)
  }

  override fun writeInt8(value: Byte) {
    output.writeByte(value.toInt())
  }

  override fun writeInt32(value: Int) {
    output.writeInt(value)
  }

  override fun writeInt64(value: Long) {
    output.writeLong(value)
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
}

@JvmInline
internal value class DataInputCodecBuffer(val input: DataInput) : ReadableOnlyCodecBuffer {
  override val position: Int
    get() = error("not supported")
  override val size: Int
    get() = Int.MAX_VALUE

  override fun readVarInt(): Int {
    return DataStreamUtils.readVarInt32(input)
  }

  override fun readVarLong(): Long {
    return DataStreamUtils.readVarInt64(input)
  }

  override fun readInt8(): Byte {
    return input.readByte()
  }

  override fun readInt32(): Int {
    return input.readInt()
  }

  override fun readInt64(): Long {
    return input.readLong()
  }

  override fun readBytes(bytes: ByteArray, offset: Int, length: Int) {
    input.readFully(bytes, offset, length)
  }

  override fun readBuffer(size: Int): ByteBuffer {
    val array = ByteArray(size)
    input.readFully(array)
    return ByteBuffer.wrap(array)
  }
}
