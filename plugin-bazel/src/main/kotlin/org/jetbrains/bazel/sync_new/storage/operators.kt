package org.jetbrains.bazel.sync_new.storage

@Suppress("UNCHECKED_CAST")
operator fun <K, V> KVStore<out K, V>.contains(key: K): Boolean = (this as KVStore<K, V>).contains(key)

@Suppress("UNCHECKED_CAST")
operator fun <K, V> KVStore<out K, V>.get(key: K): V? = (this as KVStore<K, V>).get(key)

operator fun <K, V> KVStore<K, V>.set(key: K, value: V): Unit = this.put(key, value)
