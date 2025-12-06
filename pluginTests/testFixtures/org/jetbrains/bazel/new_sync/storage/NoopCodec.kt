package org.jetbrains.bazel.new_sync.storage

import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecBuffer
import org.jetbrains.bazel.sync_new.codec.CodecContext

class NoopCodec<T> : Codec<T> {
  override fun encode(
    ctx: CodecContext,
    buffer: CodecBuffer,
    value: T,
  ) {
    error("noop")
  }

  override fun decode(ctx: CodecContext, buffer: CodecBuffer): T {
    error("noop")
  }
}
