package org.jetbrains.bazel.sync_new.index

interface One2OneIndex<K, V> : SyncIndex {
  val values: Sequence<V>
  fun set(key: K, value: V)
  fun get(key: K): V?
  fun invalidate(key: K): V?
}
