package org.jetbrains.bazel.sync_new.storage

typealias KVMultiSetMap<K, V> = KVStore<K, Set<V>>

fun <K, V> KVMultiSetMap<K, V>.put(key: K, value: V) {
  compute(key) { _, v ->
    if (v == null) {
      setOf(value)
    } else {
      v + value
    }
  }
}

fun <K, V> KVMultiSetMap<K, V>.remove(key: K, value: V) {
  compute(key) { _, v ->
    if (v == null) {
      return@compute null
    }
    val result = v - value
    if (result.isEmpty()) {
      null
    } else {
      result
    }
  }
}
