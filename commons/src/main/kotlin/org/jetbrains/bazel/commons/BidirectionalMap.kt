package org.jetbrains.bazel.commons

/**
 * Abstraction for bidirectional map to avoid direct IntelliJ dependencies
 */
interface BidirectionalMap<K, V> {
  val keys: Set<K>
  val values: Collection<V>

  operator fun get(key: K): V?
  fun getKeysByValue(value: V): List<K>
  fun put(key: K, value: V): V?
  fun putAll(map: Map<K, V>)
  fun remove(key: K): V?
  fun clear()
  fun isEmpty(): Boolean
  fun size(): Int

  companion object {
    private lateinit var factory: () -> BidirectionalMap<*, *>

    fun getInstance(): BidirectionalMap<*, *> =
      if (Companion::factory.isInitialized) factory() else throw IllegalStateException("BidirectionalMap factory not initialized")

    @Suppress("UNCHECKED_CAST")
    fun <K, V> getTypedInstance(): BidirectionalMap<K, V> =
      if (Companion::factory.isInitialized) factory() as BidirectionalMap<K, V> else throw IllegalStateException("BidirectionalMap factory not initialized")

    fun provideBidirectionalMapFactory(factoryFunction: () -> BidirectionalMap<*, *>) {
      factory = factoryFunction
    }
  }
}
