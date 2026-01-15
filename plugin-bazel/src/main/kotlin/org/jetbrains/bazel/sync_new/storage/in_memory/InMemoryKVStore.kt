package org.jetbrains.bazel.sync_new.storage.in_memory

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecBuffer
import org.jetbrains.bazel.sync_new.codec.CodecContext
import org.jetbrains.bazel.sync_new.storage.BaseKVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.FlatPersistentStore
import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.PersistentStoreOwner
import org.jetbrains.bazel.sync_new.storage.PersistentStoreWithModificationMarker
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

open class InMemoryKVStore<K, V>(
  private val owner: PersistentStoreOwner,
  override val name: String,
  private val keyCodec: Codec<K>,
  private val valueCodec: Codec<V>,
  private val disposable: Disposable,
) : KVStore<K, V>, FlatPersistentStore, PersistentStoreWithModificationMarker, Disposable {
  companion object {
    const val CODEC_VERSION: Int = 1
  }

  protected val map: ConcurrentMap<K, V> = ConcurrentHashMap()

  init {
    Disposer.register(disposable, this)
    owner.register(this)
  }

  override fun get(key: K): V? = map[key]

  override fun put(key: K, value: V) {
    map[key] = value
    wasModified = true
  }

  override fun contains(key: K): Boolean = map.containsKey(key)

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

  override fun keys(): Sequence<K> = map.keys.iterator().asSequence()
  override fun values(): Sequence<V> = map.values.iterator().asSequence()
  override fun entries(): Sequence<Pair<K, V>> = map.asSequence()
    .map { it.key to it.value }

  override fun computeIfAbsent(key: K, op: (k: K) -> V): V? {
    val value = map.computeIfAbsent(key, op)
    wasModified = true
    return value
  }

  override fun compute(key: K, op: (k: K, v: V?) -> V?): V? {
    val value = map.compute(key, op)
    wasModified = true
    return value
  }

  override fun write(ctx: CodecContext, buffer: CodecBuffer) {
    buffer.writeVarInt(CODEC_VERSION)
    val copy = map.toMap()
    buffer.writeVarInt(copy.size)
    for ((key, value) in copy) {
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
  private val disposable: Disposable,
) : BaseKVStoreBuilder<InMemoryKVStoreBuilder<K, V>, K, V>() {
  override fun build(): KVStore<K, V> {
    val store = InMemoryKVStore(
      owner = owner,
      name = name,
      keyCodec = keyCodec ?: error("Key codec must be specified"),
      valueCodec = valueCodec ?: error("Value codec must be specified"),
      disposable = disposable,
    )
    return store
  }
}
