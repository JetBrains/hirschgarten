package org.jetbrains.bazel.utils.store.codec

interface StoreCodec<T> {
  fun encode(context: StoreCodecContext, value: T, buffer: CodecBuffer)
  fun decode(context: StoreCodecContext, buffer: CodecBuffer): T
}
