package org.jetbrains.bazel.sync_new.storage

import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecBuilder
import org.jetbrains.bazel.sync_new.codec.CodecConverter

interface FlatStorage<V> {
  fun get(): V
  fun set(value: V)
  fun modify(op: (value: V) -> V)
}

interface FlatStoreBuilder<V> {
  fun withCreator(func: () -> V): FlatStoreBuilder<V>
  fun withCodec(codec: CodecBuilder.() -> Codec<V>): FlatStoreBuilder<V>
  fun build(): FlatStorage<V>
}
