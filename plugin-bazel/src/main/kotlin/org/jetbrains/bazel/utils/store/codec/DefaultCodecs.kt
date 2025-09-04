package org.jetbrains.bazel.utils.store.codec

import com.dynatrace.hash4j.hashing.HashValue128
import com.google.protobuf.Message

class ProtoStoreCodec<T : Message>(val parser: (ByteArray) -> T) : StoreCodec<T> {
  override fun encode(
    context: StoreCodecContext,
    value: T,
    buffer: CodecBuffer,
  ) {
    val bytes = value.toByteArray()
    buffer.writeVarInt(bytes.size)
    buffer.writeBytes(bytes)
  }

  override fun decode(
    context: StoreCodecContext,
    buffer: CodecBuffer,
  ): T {
    val length = buffer.readVarInt()
    val array = buffer.readBytes(length)
    return parser(array)
  }

}

class HashValue128StoreCodec : StoreCodec<HashValue128> {
  override fun encode(
    context: StoreCodecContext,
    value: HashValue128,
    buffer: CodecBuffer,
  ) {
    buffer.writeLong(value.mostSignificantBits)
    buffer.writeLong(value.leastSignificantBits)
  }

  override fun decode(
    context: StoreCodecContext,
    buffer: CodecBuffer,
  ): HashValue128 {
    val msb = buffer.readLong()
    val lsb = buffer.readLong()
    return HashValue128(msb, lsb)
  }
}
