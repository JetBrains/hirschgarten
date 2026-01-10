package org.jetbrains.bazel.sync_new.storage.mvstore

import org.h2.mvstore.MVMap
import org.jetbrains.bazel.sync_new.storage.BaseSortedKVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.CloseableIterator
import org.jetbrains.bazel.sync_new.storage.SortedKVStore
import org.jetbrains.bazel.sync_new.storage.asCloseable

open class MVStoreSortedKVStore<K, V>(
  private val map: MVMap<K, V>,
) : SortedKVStore<K, V> {
  override fun get(key: K): V? = map[key]
  override fun put(key: K, value: V) {
    map[key] = value
  }

  override fun contains(key: K): Boolean = map.containsKey(key)

  override fun remove(key: K, useReturn: Boolean): V? = map.remove(key)

  override fun clear() {
    map.clear()
  }

  override fun keys(): CloseableIterator<K> = map.cursor(null).asCloseable()

  override fun values(): CloseableIterator<V> = object : CloseableIterator<V> {
    val cursor = map.cursor(null)
    override fun next(): V {
      cursor.next()
      return cursor.value
    }

    override fun hasNext(): Boolean = cursor.hasNext()

    override fun close() {

    }
  }

  override fun iterator(): CloseableIterator<Pair<K, V>> = object : CloseableIterator<Pair<K, V>> {
    val cursor = map.cursor(null)
    override fun next(): Pair<K, V> {
      cursor.next()
      return cursor.key to cursor.value
    }

    override fun hasNext(): Boolean = cursor.hasNext()

    override fun close() {

    }
  }

  override fun computeIfAbsent(key: K, op: (k: K) -> V): V? {
    var result: V? = null
    map.operate(
      key, null,
      object : MVMap.DecisionMaker<V>() {
        override fun decide(existingValue: V?, providedValue: V?): MVMap.Decision? {
          return if (existingValue == null) {
            MVMap.Decision.PUT
          }
          else {
            result = existingValue
            MVMap.Decision.ABORT
          }
        }

        override fun <T : V?> selectValue(existingValue: T?, providedValue: T?): T? {
          @Suppress("UNCHECKED_CAST")
          val newValue = op(key) as T
          result = newValue
          return newValue
        }

      },
    )
    return result
  }

  override fun compute(key: K, op: (k: K, v: V?) -> V?): V? {
    var result: V? = null
    map.operate(
      key, null,
      object : MVMap.DecisionMaker<V>() {
        override fun decide(existingValue: V?, providedValue: V?): MVMap.Decision? {
          result = op(key, existingValue)
          if (result == null) {
            return MVMap.Decision.REMOVE
          } else {
            return MVMap.Decision.PUT
          }
        }

        override fun <T : V?> selectValue(existingValue: T?, providedValue: T?): T? {
          @Suppress("UNCHECKED_CAST")
          return result as T?
        }
      },
    )
    return result
  }
}

class MVStoreSortedKVStoreBuilder<K, V>(
  private val storageContext: MVStoreStorageContext,
  private val name: String,
  private val keyType: Class<K>,
  private val valueType: Class<V>,
) : BaseSortedKVStoreBuilder<MVStoreSortedKVStoreBuilder<K, V>, K, V>() {
  override fun build(): SortedKVStore<K, V> {
    val comparator = keyComparator?.invoke() ?: run {
      if (Comparable::class.java.isAssignableFrom(keyType)) {
        @Suppress("UNCHECKED_CAST")
        naturalOrder<Comparable<Any>>() as Comparator<K>
      }
      else {
        MVStoreComparators.getFallbackComparator(keyType) ?: error("Key type must be Comparable or a comparator must be provided")
      }
    }
    val builder = MVMap.Builder<K, V>()
    builder.setKeyType(
      MVStoreOrderableDataType(
        codec = keyCodec ?: error("Key codec must be specified"),
        type = keyType,
        comparator = comparator,
      ),
    )
    builder.setValueType(
      MVStoreDataType(
        codec = valueCodec ?: error("Value codec must be specified"),
        type = valueType,
      ),
    )
    val map = storageContext.openOrResetMap(name) { builder }
    return MVStoreSortedKVStore(map)
  }
}
