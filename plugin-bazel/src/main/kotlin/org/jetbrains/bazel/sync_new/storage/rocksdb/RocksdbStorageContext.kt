package org.jetbrains.bazel.sync_new.storage.rocksdb

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync_new.storage.FlatPersistentStore
import org.jetbrains.bazel.sync_new.storage.FlatStoreBuilder
import org.jetbrains.bazel.sync_new.storage.KVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.LifecycleStoreContext
import org.jetbrains.bazel.sync_new.storage.PersistentStoreOwner
import org.jetbrains.bazel.sync_new.storage.SortedKVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.StorageContext
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.rocksdb.Options
import org.rocksdb.RocksDB

class RocksdbStorageContext(
  internal val project: Project,
  private val disposable: Disposable,
) : StorageContext, LifecycleStoreContext, PersistentStoreOwner, Disposable {
  private val db: RocksDB

  init {
    RocksDB.loadLibrary()

    val options = Options()
      .setCreateIfMissing(true)
      .setCreateMissingColumnFamilies(true)
      .optimizeUniversalStyleCompaction()
      .setAllowConcurrentMemtableWrite(true)
      .setAllowMmapReads(true)
      .setAllowMmapWrites(true)
      .setAvoidUnnecessaryBlockingIO(true)
    db = RocksDB.open(options, "")
  }

  override fun <K, V> createKVStore(
    name: String,
    keyType: Class<K>,
    valueType: Class<V>,
    vararg hints: StorageHints,
  ): KVStoreBuilder<*, K, V> {
    TODO("Not yet implemented")
  }

  override fun <K, V> createSortedKVStore(
    name: String,
    keyType: Class<K>,
    valueType: Class<V>,
    vararg hints: StorageHints,
  ): SortedKVStoreBuilder<*, K, V> {
    TODO("Not yet implemented")
  }

  override fun <T> createFlatStore(
    name: String,
    type: Class<T>,
    vararg hints: StorageHints,
  ): FlatStoreBuilder<T> {
    TODO("Not yet implemented")
  }

  override fun save(force: Boolean) {
    TODO("Not yet implemented")
  }

  override fun register(store: FlatPersistentStore) {
    TODO("Not yet implemented")
  }

  override fun unregister(store: FlatPersistentStore) {
    TODO("Not yet implemented")
  }

  override fun dispose() {
    save(force = true)
    db.close()
  }
}
