package org.jetbrains.bazel.sync_new.storage.rocksdb

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.bazel.sync_new.storage.SortedKVStore
import org.rocksdb.ColumnFamilyHandle
import org.rocksdb.RocksDB

class RocksdbSortedKVStore<K : Any, V>(
  private val db: RocksDB,
  private val cfHandle: ColumnFamilyHandle,
  private val disposable: Disposable
) : SortedKVStore<K, V>, Disposable {

  private val cache = Caffeine.newBuilder()
    .maximumSize(10_000) // TODO: fine tune
    .weakKeys()
    .weakValues()
    .build<K, V>()

  init {
    Disposer.register(disposable, this)
  }

  override fun getHighestKey(): K? {
    TODO("Not yet implemented")
  }

  override fun get(key: K): V? {
    TODO("Not yet implemented")
  }

  override fun put(key: K, value: V) {
    TODO("Not yet implemented")
  }

  override fun contains(key: K): Boolean {
    TODO("Not yet implemented")
  }

  override fun remove(key: K, useReturn: Boolean): V? {
    TODO("Not yet implemented")
  }

  override fun clear() {
    TODO("Not yet implemented")
  }

  override fun keys(): Sequence<K> {
    TODO("Not yet implemented")
  }

  override fun values(): Sequence<V> {
    TODO("Not yet implemented")
  }

  override fun asSequence(): Sequence<Pair<K, V>> {
    TODO("Not yet implemented")
  }

  override fun computeIfAbsent(key: K, op: (k: K) -> V): V? {
    TODO("Not yet implemented")
  }

  override fun compute(key: K, op: (k: K, v: V?) -> V?): V? {
    TODO("Not yet implemented")
  }

  override fun dispose() {
    cfHandle.close()
  }
}
