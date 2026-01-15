package org.jetbrains.bazel.new_sync.storage

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.junit5.fixture.TestFixture
import com.intellij.testFramework.junit5.fixture.testFixture
import org.jetbrains.bazel.sync_new.storage.FlatPersistentStore
import org.jetbrains.bazel.sync_new.storage.FlatStorage
import org.jetbrains.bazel.sync_new.storage.FlatStoreBuilder
import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.KVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.PersistentStoreOwner
import org.jetbrains.bazel.sync_new.storage.StorageContext
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.in_memory.InMemoryFlatStoreBuilder
import org.jetbrains.bazel.sync_new.storage.in_memory.InMemoryKVStoreBuilder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

fun storageContextFixture(): TestFixture<TestStorageContext> = testFixture {
  val disposable = Disposer.newDisposable()
  val ctx = TestStorageContext(disposable)
  initialized(ctx) {
    Disposer.dispose(disposable)
  }
}

class TestStorageContext(private val disposable: Disposable) : StorageContext, PersistentStoreOwner {
  val cleaners: ConcurrentMap<Any, () -> Unit> = ConcurrentHashMap()

  override fun <K : Any, V : Any> createKVStore(
    name: String,
    keyType: Class<K>,
    valueType: Class<V>,
    vararg hints: StorageHints,
  ): KVStoreBuilder<*, K, V> = InMemoryKVStoreBuilder(this, name, disposable)

  override fun <T : Any> createFlatStore(
    name: String,
    type: Class<T>,
    vararg hints: StorageHints,
  ): FlatStoreBuilder<T> = InMemoryFlatStoreBuilder(this, name)

  override fun register(store: FlatPersistentStore) {
    cleaners[store] = {
      when (store) {
        is KVStore<*, *> -> store.clear()
        is FlatStorage<*> -> store.reset()
      }
    }
  }

  override fun unregister(store: FlatPersistentStore) {
    cleaners.remove(store)
  }

  fun clearAll() {
    cleaners.values.forEach { it() }
  }

}
