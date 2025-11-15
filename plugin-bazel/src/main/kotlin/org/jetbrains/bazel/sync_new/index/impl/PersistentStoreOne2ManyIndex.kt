package org.jetbrains.bazel.sync_new.index.impl

import org.jetbrains.bazel.sync_new.index.One2ManyIndex
import org.jetbrains.bazel.sync_new.storage.KVStore

class PersistentStoreOne2ManyIndex<K, V>(
  val storage: KVStore<K, MutableSet<V>>,
  val setCreator: () -> MutableSet<V> = { mutableSetOf() },
) : One2ManyIndex<K, V> {
  override fun add(key: K, value: Iterable<V>) {
    storage.computeIfAbsent(key) { setCreator() }?.addAll(value)
  }

  override fun invalidateByKey(key: K): Sequence<V> {
    return storage.remove(key, useReturn = true)?.asSequence()
      ?: return emptySequence()
  }

  override fun invalidateAll() {
    storage.clear()
  }
}
