package org.jetbrains.bazel.sync_new.storage.intellij

import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.KeyDescriptor
import com.intellij.util.io.PersistentMapBuilder
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.codec.CodecBuilder
import org.jetbrains.bazel.sync_new.storage.BaseKVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.CloseableIterator
import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.KVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.asCloseableIterator
import java.io.DataInput
import java.io.DataOutput
import java.nio.file.Path

internal class IntellijPersistentMapKVStore<K : Any, V : Any>(
  private val context: IntellijStorageContext,
  private val file: Path,
  private val keyCodec: Codec<K>,
  private val valueCodec: Codec<V>,
) : KVStore<K, V>, AutoCloseable {
  private val map = run {
    val keyDesc = object : KeyDescriptor<K> {
      override fun getHashCode(value: K): Int = value.hashCode()

      override fun save(out: DataOutput, value: K) {
        keyCodec.encode(JavaDataCodecContext, DataOutputCodecBuffer(out), value)
      }

      override fun isEqual(val1: K, val2: K): Boolean = val1 == val2

      override fun read(`in`: DataInput): K {
        return keyCodec.decode(JavaDataCodecContext, DataInputCodecBuffer(`in`))
      }
    }
    val valueCodec = object : DataExternalizer<V> {
      override fun save(out: DataOutput, value: V) {
        valueCodec.encode(JavaDataCodecContext, DataOutputCodecBuffer(out), value)
      }

      override fun read(`in`: DataInput): V {
        return valueCodec.decode(JavaDataCodecContext, DataInputCodecBuffer(`in`))
      }

    }
    PersistentMapBuilder.newBuilder(file, keyDesc, valueCodec)
      .withWal(false)
      .withCompactOnClose(true)
      .build()
  }

  init {
    context.register(this)
  }

  override fun get(key: K): V? {
    return map[key]
  }

  override fun put(key: K, value: V) {
    map.put(key, value)
  }

  override fun contains(key: K): Boolean {
    return map.containsMapping(key)
  }


  override fun remove(key: K, useReturn: Boolean): V? {
    val value = if (useReturn) {
      map[key]
    }
    else {
      null
    }
    map.remove(key)
    return value;
  }

  override fun clear() {
    val toRemove = mutableListOf<K>()
    map.processKeysWithExistingMapping {
      toRemove.add(it)
      true
    }
    toRemove.forEach { map.remove(it) }
  }

  override fun keys(): Sequence<K> {
    val result = mutableListOf<K>()
    map.processKeysWithExistingMapping {
      result.add(it);
      true
    }
    return result.asSequence()
  }

  override fun values(): Sequence<V> {
    val result = mutableListOf<V>()
    map.processKeysWithExistingMapping {
      result.add(map.get(it))
      true
    }
    return result.asSequence()
  }

  override fun entries(): Sequence<Pair<K, V>> {
    val result = mutableListOf<Pair<K, V>>()
    map.processKeysWithExistingMapping {
      result.add(Pair(it, map.get(it)))
      true
    }
    return result.asSequence()
  }

  override fun computeIfAbsent(key: K, op: (k: K) -> V): V? {
    val existing = map[key]
    if (existing != null) {
      return existing
    }
    val computed = op(key)
    map.put(key, computed)
    return computed
  }

  override fun compute(key: K, op: (k: K, v: V?) -> V?): V? {
    val existing = map[key]
    val computed = op(key, existing)
    if (computed != null) {
      map.put(key, computed)
    }
    else {
      map.remove(key)
    }
    return computed
  }

  override fun close() {
    map.close()
  }

}

internal class IntellijPersistentMapKVStoreBuilder<K : Any, V : Any>(
  private val context: IntellijStorageContext,
  private val file: Path
) :
  BaseKVStoreBuilder<IntellijPersistentMapKVStoreBuilder<K, V>, K, V>() {
  override fun build(): KVStore<K, V> {
    return IntellijPersistentMapKVStore(
      context = context,
      file = file,
      keyCodec = keyCodec ?: error("key codec must be set"),
      valueCodec = valueCodec ?: error("value codec must be set"),
    )
  }

}
