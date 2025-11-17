package org.jetbrains.bazel.sync_new.index.impl

import org.jetbrains.bazel.sync_new.index.One2ManyIndex
import org.jetbrains.bazel.sync_new.storage.KVStore

class PersistentStoreOne2ManyIndex<K, V>(
  val storage: KVStore<K, Set<V>>,
) : One2ManyIndex<K, V> {
  override fun add(key: K, value: Iterable<V>) {
    storage.compute(key) { _, set ->
      if (set == null) {
        value.toSet()
      } else {
        set + value
      }
    }
  }

  override fun get(key: K): Sequence<V> = storage.get(key)?.asSequence() ?: emptySequence()

  override fun invalidate(key: K): Sequence<V> {
    return storage.remove(key, useReturn = true)?.asSequence()
      ?: return emptySequence()
  }

  override fun invalidate(key: K, value: V) {
    storage.compute(key) { _, set ->
      if (set == null) {
        null
      } else {
        set - value
      }
    }
  }

  override fun invalidateAll() {
    storage.clear()
  }
}
