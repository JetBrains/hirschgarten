package org.jetbrains.bazel.sync_new.util

import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.SetMultimap

operator fun <K, V> SetMultimap<K, V>.plus(other: SetMultimap<K, V>): SetMultimap<K, V> {
  val map = HashMultimap.create<K, V>()
  map.putAll(this)
  map.putAll(other)
  return map
}

operator fun <K, V> Multimap<K, V>.iterator(): Iterator<Map.Entry<K, Collection<V>>> = this.asMap().iterator()


