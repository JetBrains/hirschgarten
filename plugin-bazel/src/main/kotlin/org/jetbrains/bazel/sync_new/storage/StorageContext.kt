package org.jetbrains.bazel.sync_new.storage

interface StorageContext {
  fun <K : Any, V : Any> createKVStore(
    name: String,
    keyType: Class<K>,
    valueType: Class<V>,
    vararg hints: StorageHints,
  ): KVStoreBuilder<*, K, V>

  fun <K : Any, V : Any> createSortedKVStore(
    name: String,
    keyType: Class<K>,
    valueType: Class<V>,
    vararg hints: StorageHints,
  ): SortedKVStoreBuilder<*, K, V>

  fun <T : Any> createFlatStore(
    name: String,
    type: Class<T>,
    vararg hints: StorageHints,
  ): FlatStoreBuilder<T>
}

interface LifecycleStoreContext {
  fun save(force: Boolean = false)
}

interface CompactingStoreContext {
  suspend fun compact()
}

inline fun <reified K : Any, reified V : Any> StorageContext.createKVStore(name: String, vararg hints: StorageHints): KVStoreBuilder<*, K, V> =
  createKVStore(name, K::class.java, V::class.java, *hints)

inline fun <reified K : Any, reified V : Any> StorageContext.createSortedKVStore(name: String, vararg hints: StorageHints): SortedKVStoreBuilder<*, K, V> =
  createSortedKVStore(name, K::class.java, V::class.java, *hints)

inline fun <reified T : Any> StorageContext.createFlatStore(name: String, vararg hints: StorageHints): FlatStoreBuilder<T> =
  createFlatStore(name, T::class.java, *hints)
