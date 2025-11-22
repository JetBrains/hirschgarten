package org.jetbrains.bazel.sync_new.storage.in_memory

import com.intellij.openapi.Disposable
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecBuffer
import org.jetbrains.bazel.sync_new.codec.CodecContext
import org.jetbrains.bazel.sync_new.storage.BaseKVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.BaseSortedKVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.FlatPersistentStore
import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.PersistentStoreOwner
import org.jetbrains.bazel.sync_new.storage.PersistentStoreWithModificationMarker
import org.jetbrains.bazel.sync_new.storage.SortedKVStore
import java.util.NavigableMap
import java.util.SortedMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentSkipListMap

open class InMemoryKVStore<K, V>(
  private val owner: PersistentStoreOwner,
  override val name: String,
  private val keyCodec: Codec<K>,
  private val valueCodec: Codec<V>,
) : KVStore<K, V>, FlatPersistentStore, PersistentStoreWithModificationMarker, Disposable {
  companion object {
    const val CODEC_VERSION: Int = 1
  }

  protected val map: ConcurrentMap<K, V> by lazy { createInternalMap() }

  protected open fun createInternalMap(): ConcurrentMap<K, V> = ConcurrentHashMap()

  init {
    owner.register(this)
  }

  override fun get(key: K): V? = map[key]

  override fun set(key: K, value: V) {
    map[key] = value
    wasModified = true
  }

  override fun has(key: K): Boolean = map.containsKey(key)

  override fun remove(key: K, useReturn: Boolean): V? {
    val value = map.remove(key)
    if (value != null) {
      wasModified = true
    }
    return value
  }

  override fun clear() {
    map.clear()
    wasModified = true
  }

  override fun keys(): Sequence<K> = map.keys.asSequence()

  override fun values(): Sequence<V> = map.values.asSequence()
  override fun computeIfAbsent(key: K, op: (k: K) -> V): V? = map.computeIfAbsent(key, op)
  override fun compute(key: K, op: (k: K, v: V?) -> V?): V? = map.compute(key, op)

  override fun write(ctx: CodecContext, buffer: CodecBuffer) {
    buffer.writeVarInt(CODEC_VERSION)
    buffer.writeVarInt(map.size)
    for ((key, value) in map) {
      keyCodec.encode(ctx, buffer, key)
      valueCodec.encode(ctx, buffer, value)
    }
    wasModified = false
  }

  override fun read(ctx: CodecContext, buffer: CodecBuffer) {
    check(buffer.readVarInt() == CODEC_VERSION) { "unsupported version version" }
    val size = buffer.readVarInt()
    for (n in 0 until size) {
      val key = keyCodec.decode(ctx, buffer)
      val value = valueCodec.decode(ctx, buffer)
      map[key] = value
    }
  }

  @field:Volatile
  override var wasModified: Boolean = false

  override fun dispose() {
    owner.unregister(this)
  }

}

class InMemoryKVStoreBuilder<K, V>(
  private val owner: PersistentStoreOwner,
  private val name: String,
) : BaseKVStoreBuilder<InMemoryKVStoreBuilder<K, V>, K, V>() {
  override fun build(): KVStore<K, V> {
    val store = InMemoryKVStore(
      owner = owner,
      name = name,
      keyCodec = keyCodec ?: error("Key codec must be specified"),
      valueCodec = valueCodec ?: error("Value codec must be specified"),
    )
    return store
  }
}

open class InMemorySortedKVStore<K, V>(
  owner: PersistentStoreOwner,
  name: String,
  keyCodec: Codec<K>,
  valueCodec: Codec<V>,
  private val comparator: Comparator<K>,
) : InMemoryKVStore<K, V>(owner, name, keyCodec, valueCodec), SortedKVStore<K, V> {
  protected val sortedMap: SortedMap<K, V>
    get() = map as NavigableMap<K, V>

  override fun createInternalMap(): ConcurrentMap<K, V> = ConcurrentSkipListMap(comparator)

  override fun getHighestKey(): K? = sortedMap.lastKey()
}

class InMemorySortedKVStoreBuilder<K, V>(
  private val owner: PersistentStoreOwner,
  private val name: String,
) :
  BaseSortedKVStoreBuilder<InMemorySortedKVStoreBuilder<K, V>, K, V>() {
  override fun build(): SortedKVStore<K, V> = InMemorySortedKVStore(
    owner = owner,
    name = name,
    keyCodec = keyCodec ?: error("Key codec must be specified"),
    valueCodec = valueCodec ?: error("Value codec must be specified"),
    comparator = keyComparator?.invoke() ?: error("Comparator must be specified"),
  )
}
