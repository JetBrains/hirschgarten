package org.jetbrains.bazel.commons

/**
 * Abstraction for bidirectional map to avoid direct IntelliJ dependencies
 */
interface BidirectionalMap<K, V> : MutableMap<K, V> {
  fun getKeysByValue(value: V): List<K>

  companion object {
    private lateinit var factory: () -> BidirectionalMap<*, *>

    fun getInstance(): BidirectionalMap<*, *> = factory()

    @Suppress("UNCHECKED_CAST")
    fun <K, V> getTypedInstance(): BidirectionalMap<K, V> = factory() as BidirectionalMap<K, V>

    fun provideBidirectionalMapFactory(factoryFunction: () -> BidirectionalMap<*, *>) {
      factory = factoryFunction
    }
  }
}
