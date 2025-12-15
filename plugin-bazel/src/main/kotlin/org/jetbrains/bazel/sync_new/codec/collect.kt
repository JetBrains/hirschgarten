package org.jetbrains.bazel.sync_new.codec

inline fun <T> CodecBuilder.ofSet(
  elementCodec: Codec<T>,
  crossinline setFactory: () -> MutableSet<T> = { mutableSetOf() },
): Codec<Set<T>> = codecOf(
  encode = { ctx, buffer, value ->
    buffer.writeVarInt(value.size)
    for (element in value) {
      elementCodec.encode(ctx, buffer, element)
    }
  },
  decode = { ctx, buffer ->
    val size = buffer.readVarInt()
    val result = setFactory()
    repeat(size) {
      result.add(elementCodec.decode(ctx, buffer))
    }
    result
  },
)

object IntArrayCodec {
  fun encode(
    ctx: CodecContext,
    buffer: CodecBuffer,
    value: IntArray,
  ) {
    buffer.writeVarInt(value.size)
    if (buffer is HasByteBuffer) {
      buffer.reserve(value.size * Int.SIZE_BYTES)
      buffer.buffer.asIntBuffer().put(value)
      buffer.buffer.position(buffer.buffer.position() + value.size * Int.SIZE_BYTES)
    } else {
      for (it in value.indices) {
        buffer.writeInt32(value[it])
      }
    }
  }

  fun decode(
    ctx: CodecContext,
    buffer: CodecBuffer,
  ): IntArray {
    val length = buffer.readVarInt()
    val result = IntArray(length)
    if (buffer is HasByteBuffer) {
      buffer.buffer.asIntBuffer().get(result)
    } else {
      for (n in 0 until length) {
        result[n] = buffer.readInt32()
      }
    }
    return result
  }
}

object LongArrayCodec {
  fun encode(
    ctx: CodecContext,
    buffer: CodecBuffer,
    value: LongArray,
  ) {
    buffer.writeVarInt(value.size)
    if (buffer is HasByteBuffer) {
      buffer.reserve(value.size * Long.SIZE_BYTES)
      buffer.buffer.asLongBuffer().put(value)
      buffer.buffer.position(buffer.buffer.position() + value.size * Long.SIZE_BYTES)
    } else {
      for (it in value.indices) {
        buffer.writeInt64(value[it])
      }
    }
  }

  fun decode(
    ctx: CodecContext,
    buffer: CodecBuffer,
  ): LongArray {
    val length = buffer.readVarInt()
    val result = LongArray(length)
    if (buffer is HasByteBuffer) {
      buffer.buffer.asLongBuffer().get(result)
    } else {
      for (n in 0 until length) {
        result[n] = buffer.readInt64()
      }
    }
    return result
  }
}


