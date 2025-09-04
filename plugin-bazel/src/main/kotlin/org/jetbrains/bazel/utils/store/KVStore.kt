package org.jetbrains.bazel.utils.store

interface KVStore<K, V> {
  fun get(key: K): V?
  fun put(key: K, value: V): V?
  fun has(key: K): Boolean
  fun remove(key: K): V?
  fun clear()
  fun keys(): Sequence<K>
  fun values(): Sequence<V>
  fun size(): Int
}
