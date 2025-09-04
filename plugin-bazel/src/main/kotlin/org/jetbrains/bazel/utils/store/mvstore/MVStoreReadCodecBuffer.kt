package org.jetbrains.bazel.utils.store.mvstore

import org.h2.mvstore.DataUtils
import org.jetbrains.bazel.utils.store.codec.CodecBuffer
import java.nio.ByteBuffer

@JvmInline
value class MVStoreReadCodecBuffer(val buffer: ByteBuffer) : CodecBuffer {
  override fun isReadable(): Boolean = true

  override fun read(): Byte = buffer.get()

  override fun readShort(): Short = buffer.getShort()

  override fun readInt(): Int = buffer.getInt()

  override fun readLong(): Long = buffer.getLong()
  override fun readBytes(length: Int): ByteArray {
    val array = ByteArray(length)
    buffer.get(array)
    return array
  }

  override fun readString(length: Int): String {
    val length = readVarInt()
    val array = readBytes(length)
    return array.decodeToString()
  }

  override fun readVarInt(): Int = DataUtils.readVarInt(this.buffer)
  override fun readVarLong(): Long = DataUtils.readVarLong(this.buffer)

  override fun write(value: Byte) = error("not supported")
  override fun writeShort(value: Short) = error("not supported")
  override fun writeInt(value: Int) = error("not supported")
  override fun writeLong(value: Long) = error("not supported")
  override fun writeBytes(value: ByteArray) = error("not supported")
  override fun writeString(value: String) = error("not supported")
  override fun writeVarInt(value: Int) = error("not supported")
  override fun writeVarLong(value: Long) = error("not supported")
}
