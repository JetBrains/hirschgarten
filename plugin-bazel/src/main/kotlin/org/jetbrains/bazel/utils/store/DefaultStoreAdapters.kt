package org.jetbrains.bazel.utils.store

import com.dynatrace.hash4j.hashing.HashValue128

class Key2HashValue128KVStoreAdapter<K, V>(
  private val hasher: (K) -> HashValue128,
  private val inner: KVStore<HashValue128, V>,
) : KVStore<K, V> {
  override fun get(key: K): V? = inner.get(hasher(key))

  override fun put(key: K, value: V): V? = inner.put(hasher(key), value)

  override fun has(key: K): Boolean = inner.has(hasher(key))

  override fun remove(key: K): V? = inner.remove(hasher(key))

  override fun clear() = inner.clear()

  override fun keys(): Sequence<K> = error("${this::class} only store key hashes")

  override fun values(): Sequence<V> = inner.values()

  override fun size(): Int = inner.size()
}
