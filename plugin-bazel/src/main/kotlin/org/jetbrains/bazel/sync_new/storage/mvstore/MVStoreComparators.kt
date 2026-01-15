package org.jetbrains.bazel.sync_new.storage.mvstore

import com.dynatrace.hash4j.hashing.HashValue128
import org.jetbrains.bazel.sync_new.storage.hash.hash128Comparator

/**
MVStore doesn't support BTree creation with non-natural comparators (as expected),
due to this issue storage context cannot create KVStore and have to fall back to KVSortedStore,
which is only MVStore KVStore implementation.

To partially solve that issue we introduce custom comparators for the most commonly used key data types.
 */
object MVStoreComparators {
  val comparators: Map<Class<*>, Comparator<*>> = mapOf(HashValue128::class.java to hash128Comparator)

  @Suppress("UNCHECKED_CAST")
  fun <T> getFallbackComparator(type: Class<T>): Comparator<T>? = comparators[type] as Comparator<T>?
}
