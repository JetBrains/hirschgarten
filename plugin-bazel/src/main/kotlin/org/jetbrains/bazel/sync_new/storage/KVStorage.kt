package org.jetbrains.bazel.sync_new.storage

import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecBuilder
import org.jetbrains.bazel.sync_new.storage.bsearch.BinarySearch

interface KVStorage<K, V> {
  fun get(key: K): V?
  fun set(key: K, value: V)
  fun has(key: K): Boolean
  fun remove(key: K, output_previous: Boolean = false): V?
  fun clear()
  fun keys(): Sequence<K>
  fun values(): Sequence<V>
}

interface KVStoreBuilder<K, V> {
  fun withKeyCodec(codec: CodecBuilder.() -> Codec<K>, search: () -> BinarySearch<K>): KVStoreBuilder<K, V>
  fun withValueCodec(codec: CodecBuilder.() -> Codec<V>): KVStoreBuilder<K, V>
  fun build(): KVStorage<K, V>
}
