package org.jetbrains.bazel.sync_new.index

interface One2ManyIndex<K, V> : Index {
  fun add(key: K, value: Iterable<V>)
  fun invalidateByKey(key: K): Sequence<V>
}
