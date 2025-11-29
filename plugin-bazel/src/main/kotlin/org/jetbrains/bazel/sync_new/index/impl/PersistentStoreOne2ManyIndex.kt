package org.jetbrains.bazel.sync_new.index.impl

import com.intellij.openapi.Disposable
import org.jetbrains.bazel.sync_new.index.One2ManyIndex
import org.jetbrains.bazel.sync_new.index.SyncIndexContext
import org.jetbrains.bazel.sync_new.index.SyncIndexUtils
import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.StorageContext

private class PersistentStoreOne2ManyIndex<K, V>(
  val owner: SyncIndexContext,
  override val name: String,
  val storage: KVStore<K, Set<V>>,
) : One2ManyIndex<K, V>, Disposable {
  override fun add(key: K, value: Iterable<V>) {
    storage.compute(key) { _, set ->
      if (set == null) {
        value.toSet()
      } else {
        set + value
      }
    }
  }

  override fun set(key: K, value: Iterable<V>) {
    storage.put(key, value.toSet())
  }

  override fun get(key: K): Sequence<V> = storage.get(key)?.asSequence() ?: emptySequence()

  override fun invalidate(key: K): Sequence<V> {
    return storage.remove(key, useReturn = true)?.asSequence()
      ?: return emptySequence()
  }

  override fun invalidate(key: K, value: V) {
    storage.compute(key) { _, set ->
      if (set == null) {
        return@compute null
      }
      val result = set - value
      if (result.isEmpty()) {
        null
      } else {
        result
      }
    }
  }

  override fun invalidateAll() {
    storage.clear()
  }

  override fun dispose() {
    owner.unregister(this)
  }
}

fun <K, V> SyncIndexContext.createOne2ManyIndex(
  name: String,
  store: (name: String, storage: StorageContext) -> KVStore<K, Set<V>>,
): One2ManyIndex<K, V> {
  val store = store(SyncIndexUtils.toStorageName(name), storageContext)
  return PersistentStoreOne2ManyIndex(this, name, store)
    .also(this::register)
}
