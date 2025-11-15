package org.jetbrains.bazel.sync_new.storage

interface StorageContext {
  fun <K, V> createKVStorage(
    name: String,
    keyType: Class<K>,
    valueType: Class<V>,
    vararg hints: StorageHints,
  ): KVStoreBuilder<*, K, V>

  fun <K, V> createSortedKVStorage(
    name: String,
    keyType: Class<K>,
    valueType: Class<V>,
    vararg hints: StorageHints,
  ): SortedKVStoreBuilder<*, K, V>

  fun <T> createFlatStorage(
    name: String,
    type: Class<T>,
    vararg hints: StorageHints,
  ): FlatStoreBuilder<T>
}

interface LifecycleStoreContext {
  fun onSave()
}

inline fun <reified K, reified V> StorageContext.createKVStorage(name: String, vararg hints: StorageHints): KVStoreBuilder<*, K, V> =
  createKVStorage(name, K::class.java, V::class.java, *hints)

inline fun <reified K, reified V> StorageContext.createSortedKVStorage(name: String, vararg hints: StorageHints): SortedKVStoreBuilder<*, K, V> =
  createSortedKVStorage(name, K::class.java, V::class.java, *hints)

inline fun <reified T> StorageContext.createFlatStorage(name: String, vararg hints: StorageHints): FlatStoreBuilder<T> =
  createFlatStorage(name, T::class.java, *hints)
