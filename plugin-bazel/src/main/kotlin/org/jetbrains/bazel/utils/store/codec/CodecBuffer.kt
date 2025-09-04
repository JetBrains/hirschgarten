package org.jetbrains.bazel.utils.store.codec

interface CodecBuffer {
  // control
  fun isReadable(): Boolean

  // reading
  fun read(): Byte
  fun readShort(): Short
  fun readInt(): Int
  fun readLong(): Long
  fun readBytes(length: Int): ByteArray
  fun readString(length: Int): String

  fun readVarInt(): Int
  fun readVarLong(): Long

  // writing
  fun write(value: Byte)
  fun writeShort(value: Short)
  fun writeInt(value: Int)
  fun writeLong(value: Long)
  fun writeBytes(value: ByteArray)
  fun writeString(value: String)

  fun writeVarInt(value: Int)
  fun writeVarLong(value: Long)
}
