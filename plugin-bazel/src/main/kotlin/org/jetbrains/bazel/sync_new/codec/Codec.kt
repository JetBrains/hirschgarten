package org.jetbrains.bazel.sync_new.codec

interface Codec<T> {
  fun encode(ctx: CodecContext, buffer: CodecBuffer, value: T)
  fun decode(ctx: CodecContext, buffer: CodecBuffer): T
  fun getSize(ctx: CodecContext, value: T): Int = -1
}

inline fun <T> codecOf(
  crossinline encode: (ctx: CodecContext, buffer: CodecBuffer, value: T) -> Unit,
  crossinline decode: (ctx: CodecContext, buffer: CodecBuffer) -> T,
  crossinline size: (ctx: CodecContext, value: T) -> Int = { _, _ -> -1 },
): Codec<T> = object : Codec<T> {
  override fun encode(ctx: CodecContext, buffer: CodecBuffer, value: T) = encode(ctx, buffer, value)
  override fun decode(ctx: CodecContext, buffer: CodecBuffer): T = decode(ctx, buffer)
  override fun getSize(ctx: CodecContext, value: T): Int = size(ctx, value)
}

inline fun <T> versionedCodecOf(
  version: Int,
  crossinline encode: (ctx: CodecContext, buffer: CodecBuffer, value: T) -> Unit,
  crossinline decode: (ctx: CodecContext, buffer: CodecBuffer, version: Int) -> T,
  crossinline size: (ctx: CodecContext, value: T) -> Int = { _, _ -> -1 },
): Codec<T> = codecOf(
  encode = { ctx, buffer, value ->
    buffer.writeVarInt(version)
    encode(ctx, buffer, value)
  },
  decode = { ctx, buffer ->
    val v = buffer.readVarInt()
    decode(ctx, buffer, v)
  },
  size = size
)

fun <U, V> Codec<U>.withConverter(converter: CodecConverter<U, V>): Codec<V> =
  object : Codec<V> {
    override fun encode(ctx: CodecContext, buffer: CodecBuffer, value: V) = this@withConverter.encode(ctx, buffer, converter.from(value))
    override fun decode(ctx: CodecContext, buffer: CodecBuffer): V = this@withConverter.decode(ctx, buffer).let(converter::to)
  }

fun <U, V> Codec<U>.withConverter(converter: () -> CodecConverter<U, V>): Codec<V> = withConverter(converter())
