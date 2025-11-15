package org.jetbrains.bazel.sync_new.index

interface One2OneIndex<K, V> : Index {
  fun set(key: K, value: V)
  fun invalidate(key: K): V?
}
