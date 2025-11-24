package org.jetbrains.bazel.sync_new.codec

inline fun <T> CodecBuilder.ofSet(
  elementCodec: Codec<T>,
  crossinline setFactory: () -> MutableSet<T> = { mutableSetOf() },
) : Codec<Set<T>> = codecOf(
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
  }
)
