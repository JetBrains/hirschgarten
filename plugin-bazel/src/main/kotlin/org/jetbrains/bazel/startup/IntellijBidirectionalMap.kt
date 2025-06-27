package org.jetbrains.bazel.startup

import org.jetbrains.bazel.commons.BidirectionalMap
import com.intellij.util.containers.BidirectionalMap as IntellijBidirectionalMap

class IntellijBidirectionalMap<K, V> : BidirectionalMap<K, V> {
  private val delegate = IntellijBidirectionalMap<K, V>()

  override val keys: Set<K> get() = delegate.keys
  override val values: Collection<V> get() = delegate.values

  override fun get(key: K): V? = delegate[key]

  override fun getKeysByValue(value: V): List<K> = delegate.getKeysByValue(value) ?: emptyList()

  override fun put(key: K, value: V): V? = delegate.put(key, value)

  override fun putAll(map: Map<K, V>) = delegate.putAll(map)

  override fun remove(key: K): V? = delegate.remove(key)

  override fun clear() = delegate.clear()

  override fun isEmpty(): Boolean = delegate.isEmpty()

  override fun size(): Int = delegate.size

}
