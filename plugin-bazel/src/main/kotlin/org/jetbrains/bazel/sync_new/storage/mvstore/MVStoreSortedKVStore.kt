package org.jetbrains.bazel.sync_new.storage.mvstore

import org.h2.mvstore.MVMap
import org.jetbrains.bazel.sync_new.storage.BaseKVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.KVStore

open class MVStoreSortedKVStore<K, V>(
  private val map: MVMap<K, V>,
) : KVStore<K, V> {
  override fun get(key: K): V? = map[key]
  override fun put(key: K, value: V) {
    map[key] = value
  }

  override fun contains(key: K): Boolean = map.containsKey(key)

  override fun remove(key: K, useReturn: Boolean): V? = map.remove(key)

  override fun clear() {
    map.clear()
  }

  override fun keys(): Sequence<K> = map.cursor(null).asSequence()

  override fun values(): Sequence<V> = sequence {
    val cursor = map.cursor(null)
    while (cursor.hasNext()) {
      cursor.next()
      yield(cursor.value)
    }
  }

  override fun entries(): Sequence<Pair<K, V>> = sequence {
    val cursor = map.cursor(null)
    while (cursor.hasNext()) {
      cursor.next()
      yield(Pair(cursor.key, cursor.value))
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
          }
          else {
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
) : BaseKVStoreBuilder<MVStoreSortedKVStoreBuilder<K, V>, K, V>() {
  override fun build(): KVStore<K, V> {
    val comparator = run {
      if (Comparable::class.java.isAssignableFrom(keyType)) {
        @Suppress("UNCHECKED_CAST")
        naturalOrder<Comparable<Any>>() as Comparator<K>
      }
      else {
        MVStoreComparators.getFallbackComparator(keyType)
        ?: error("Key type must be Comparable or a comparator must be provided ${keyType}")
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
