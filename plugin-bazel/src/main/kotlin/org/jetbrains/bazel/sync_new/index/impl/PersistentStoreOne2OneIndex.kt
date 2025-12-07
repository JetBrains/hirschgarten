package org.jetbrains.bazel.sync_new.index.impl

import com.intellij.openapi.Disposable
import org.jetbrains.bazel.sync_new.index.One2OneIndex
import org.jetbrains.bazel.sync_new.index.SyncIndexContext
import org.jetbrains.bazel.sync_new.index.SyncIndexUtils
import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.StorageContext

class PersistentStoreOne2OneIndex<K, V>(
  val owner: SyncIndexContext,
  override val name: String,
  val store: KVStore<K, V>,
) : One2OneIndex<K, V>, Disposable {
  override val values: Sequence<V>
    get() = store.values()

  init {
    owner.register(this)
  }

  override fun set(key: K, value: V) {
    store.put(key, value)
  }

  override fun get(key: K): V? = store.get(key)

  override fun invalidate(key: K): V? = store.remove(key)

  override fun invalidateAll() {
    store.clear()
  }

  override fun dispose() {
    owner.unregister(this)
  }
}

fun <K, V> SyncIndexContext.createOne2OneIndex(
  name: String,
  store: (name: String, storage: StorageContext) -> KVStore<K, V>,
): One2OneIndex<K, V> {
  val store = store(SyncIndexUtils.toStorageName(name), storageContext)
  return PersistentStoreOne2OneIndex(this, name, store)
}
