package org.jetbrains.bazel.sync_new.codec.proto

import com.google.protobuf.Internal
import com.google.protobuf.Message
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecBuffer
import org.jetbrains.bazel.sync_new.codec.CodecBuilder
import org.jetbrains.bazel.sync_new.codec.CodecContext

class ProtoCodec<T : Message>(private val defaultInstance: T) : Codec<T> {
  override fun encode(
    ctx: CodecContext,
    buffer: CodecBuffer,
    value: T,
  ) {
    val bytes = value.toByteArray()
    buffer.writeInt32(bytes.size)
    buffer.writeBytes(bytes)
  }

  override fun decode(ctx: CodecContext, buffer: CodecBuffer): T {
    val parser = defaultInstance.parserForType
    val length = buffer.readInt32()
    val array = ByteArray(length)
    buffer.readBytes(array)

    @Suppress("UNCHECKED_CAST")
    return parser.parseFrom(array) as T
  }
}

fun <T : Message> CodecBuilder.ofProtoMessage(message: T): ProtoCodec<T> = ProtoCodec(message)

inline fun <reified T : Message> CodecBuilder.ofProtoMessage(): ProtoCodec<T> {
  return ProtoCodec(Internal.getDefaultInstance(T::class.java))
}
