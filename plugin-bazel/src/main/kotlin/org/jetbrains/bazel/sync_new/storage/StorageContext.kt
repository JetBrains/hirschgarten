package org.jetbrains.bazel.sync_new.storage

interface StorageContext {
  fun <K, V> createKVStore(
    name: String,
    keyType: Class<K>,
    valueType: Class<V>,
    vararg hints: StorageHints,
  ): KVStoreBuilder<*, K, V>

  fun <K, V> createSortedKVStore(
    name: String,
    keyType: Class<K>,
    valueType: Class<V>,
    vararg hints: StorageHints,
  ): SortedKVStoreBuilder<*, K, V>

  fun <T> createFlatStore(
    name: String,
    type: Class<T>,
    vararg hints: StorageHints,
  ): FlatStoreBuilder<T>
}

interface LifecycleStoreContext {
  fun save(force: Boolean = false)
}

inline fun <reified K, reified V> StorageContext.createKVStore(name: String, vararg hints: StorageHints): KVStoreBuilder<*, K, V> =
  createKVStore(name, K::class.java, V::class.java, *hints)

inline fun <reified K, reified V> StorageContext.createSortedKVStore(name: String, vararg hints: StorageHints): SortedKVStoreBuilder<*, K, V> =
  createSortedKVStore(name, K::class.java, V::class.java, *hints)

inline fun <reified T> StorageContext.createFlatStore(name: String, vararg hints: StorageHints): FlatStoreBuilder<T> =
  createFlatStore(name, T::class.java, *hints)
