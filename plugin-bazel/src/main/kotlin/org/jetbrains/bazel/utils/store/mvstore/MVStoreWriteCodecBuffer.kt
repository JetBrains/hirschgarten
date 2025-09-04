package org.jetbrains.bazel.utils.store.mvstore

import org.h2.mvstore.WriteBuffer
import org.jetbrains.bazel.utils.store.codec.CodecBuffer

@JvmInline
value class MVStoreWriteCodecBuffer(val inner: WriteBuffer) : CodecBuffer {
  override fun isReadable(): Boolean = true

  override fun read(): Byte = error("not supported")
  override fun readShort(): Short = error("not supported")
  override fun readInt(): Int = error("not supported")
  override fun readLong(): Long = error("not supported")
  override fun readBytes(length: Int): ByteArray = error("not supported")
  override fun readString(length: Int): String = error("not supported")
  override fun readVarInt(): Int = error("not supported")
  override fun readVarLong(): Long = error("not supported")

  override fun write(value: Byte) {
    inner.put(value)
  }

  override fun writeShort(value: Short) {
    inner.putShort(value)
  }

  override fun writeInt(value: Int) {
    inner.putInt(value)
  }

  override fun writeLong(value: Long) {
    inner.putLong(value)
  }

  override fun writeBytes(value: ByteArray) {
    inner.put(value)
  }

  override fun writeString(value: String) {
    inner.putVarInt(value.length).put(value.encodeToByteArray())
  }

  override fun writeVarInt(value: Int) {
    inner.putVarInt(value)
  }

  override fun writeVarLong(value: Long) {
    inner.putVarLong(value)
  }
}
