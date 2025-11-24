package org.jetbrains.bazel.sync_new.index

interface One2ManyIndex<K, V> : SyncIndex {
  fun add(key: K, value: Iterable<V>)
  fun set(key: K, value: Iterable<V>)
  fun get(key: K): Sequence<V>
  fun invalidate(key: K): Sequence<V>
  fun invalidate(key: K, value: V)
}
