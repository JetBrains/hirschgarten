package org.jetbrains.bazel.sync_new.storage

import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecBuilder
import org.jetbrains.bazel.sync_new.codec.codecBuilderOf

interface KVStore<K, V> {
  operator fun get(key: K): V?
  fun put(key: K, value: V)
  fun contains(key: K): Boolean
  fun remove(key: K, useReturn: Boolean = false): V?
  fun clear()
  fun keys(): Sequence<K>
  fun values(): Sequence<V>
  fun entries(): Sequence<Pair<K, V>>

  fun computeIfAbsent(key: K, op: (k: K) -> V): V?
  fun compute(key: K, op: (k: K, v: V?) -> V?): V?

}

interface KVStoreBuilder<B, K, V>
  where B : KVStoreBuilder<B, K, V> {
  fun withKeyCodec(codec: CodecBuilder.() -> Codec<K>): B
  fun withValueCodec(codec: CodecBuilder.() -> Codec<V>): B
  fun build(): KVStore<K, V>
}

abstract class BaseKVStoreBuilder<B, K, V> : KVStoreBuilder<B, K, V>
  where B : BaseKVStoreBuilder<B, K, V> {
  protected var keyCodec: Codec<K>? = null
  protected var valueCodec: Codec<V>? = null

  @Suppress("UNCHECKED_CAST")
  protected val thisBuilder: B = this as B

  override fun withKeyCodec(codec: CodecBuilder.() -> Codec<K>): B {
    keyCodec = codec(codecBuilderOf())
    return thisBuilder
  }

  override fun withValueCodec(codec: CodecBuilder.() -> Codec<V>): B {
    valueCodec = codec(codecBuilderOf())
    return thisBuilder
  }
}
