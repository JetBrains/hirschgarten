package org.jetbrains.bazel.sync_new.codec

interface Codec<T> {
  fun encode(ctx: CodecContext, buffer: CodecBuffer, value: T)
  fun decode(ctx: CodecContext, buffer: CodecBuffer): T
}

fun <T> codecOf(
  encode: (ctx: CodecContext, buffer: CodecBuffer, value: T) -> Unit,
  decode: (ctx: CodecContext, buffer: CodecBuffer) -> T,
): Codec<T> = object : Codec<T> {
  override fun encode(ctx: CodecContext, buffer: CodecBuffer, value: T) = encode(ctx, buffer, value)
  override fun decode(ctx: CodecContext, buffer: CodecBuffer): T = decode(ctx, buffer)
}

fun <U, V> Codec<U>.withConverter(converter: CodecConverter<U, V>): Codec<V> =
  object : Codec<V> {
    override fun encode(ctx: CodecContext, buffer: CodecBuffer, value: V) = this@withConverter.encode(ctx, buffer, converter.from(value))
    override fun decode(ctx: CodecContext, buffer: CodecBuffer): V = this@withConverter.decode(ctx, buffer).let(converter::to)
  }

fun <U, V> Codec<U>.withConverter(converter: () -> CodecConverter<U, V>): Codec<V> = withConverter(converter())
