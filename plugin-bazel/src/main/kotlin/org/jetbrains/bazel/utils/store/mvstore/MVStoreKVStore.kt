package org.jetbrains.bazel.utils.store.mvstore

import com.intellij.openapi.diagnostic.logger
import com.intellij.util.io.mvstore.openOrResetMap
import org.h2.mvstore.MVMap
import org.h2.mvstore.MVStore
import org.h2.mvstore.type.DataType
import org.jetbrains.bazel.utils.store.KVStore
import org.jetbrains.bazel.utils.store.codec.StoreCodec

class MVStoreKVStore<K, V>(
  val store: MVStore,
  val mapName: String,
  val keyDataType: DataType<K>,
  val valueDataType: DataType<V>,
) : KVStore<K, V> {
  private val map: MVMap<K, V> = openMap()

  private fun openMap(): MVMap<K, V> {
    val builder = MVMap.Builder<K, V>()
    builder.setKeyType(keyDataType)
    builder.setValueType(valueDataType)
    return openOrResetMap(
      store = store, name = mapName, mapBuilder = builder,
      logSupplier = {
        logger<MVStoreKVStore<K, V>>()
      },
    )
  }

  override fun get(key: K): V? = map.get(key)

  override fun put(key: K, value: V): V? = map.put(key, value)

  override fun has(key: K): Boolean = map.containsKey(key)

  override fun remove(key: K): V? = map.remove(key)

  override fun clear() = map.clear()

  override fun keys(): Sequence<K> = sequence {
    val cursor = map.cursor(null)
    while (cursor.hasNext()) {
      cursor.next()
      yield(cursor.key)
    }
  }

  override fun values(): Sequence<V> = sequence {
    val cursor = map.cursor(null)
    while (cursor.hasNext()) {
      cursor.next()
      yield(cursor.value)
    }
  }

  override fun size(): Int = map.size

  companion object {
    fun <K : Comparable<K>, V> createFromCodecs(store: MVStore, name: String, keyCodec: StoreCodec<K>, valueCodec: StoreCodec<V>) =
      MVStoreKVStore(
        store = store,
        mapName = name,
        keyDataType = ComparableMVStoreCodecDataType(keyCodec),
        valueDataType = ValueMVStoreCodecDataType(valueCodec),
      )

    fun <V> createHashedFromCodecs(store: MVStore, name: String, valueCodec: StoreCodec<V>) =
      MVStoreKVStore(
        store = store,
        mapName = name,
        keyDataType = HashValue128KeyDataType,
        valueDataType = ValueMVStoreCodecDataType(valueCodec),
      )
  }
}
