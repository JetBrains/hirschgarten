package org.jetbrains.bazel.sync_new.index.impl

import org.jetbrains.bazel.sync_new.index.One2OneIndex
import org.jetbrains.bazel.sync_new.storage.KVStore

class PersistentStoreOne2OneIndex<K, V>(
  override val name: String,
  val storage: KVStore<K, V>,
) : One2OneIndex<K, V> {
  override fun set(key: K, value: V) {
    storage.set(key, value)
  }

  override fun get(key: K): V? = storage.get(key)

  override fun invalidate(key: K): V? = storage.remove(key)

  override fun invalidateAll() {
    storage.clear()
  }
}
