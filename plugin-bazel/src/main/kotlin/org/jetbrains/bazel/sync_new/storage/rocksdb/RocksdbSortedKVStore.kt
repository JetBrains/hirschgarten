package org.jetbrains.bazel.sync_new.storage.rocksdb

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.storage.SortedKVStore
import org.rocksdb.ColumnFamilyHandle
import org.rocksdb.ReadOptions
import org.rocksdb.RocksDB
import org.rocksdb.WriteBatch
import org.rocksdb.WriteOptions
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

class RocksdbSortedKVStore<K : Any, V : Any>(
  private val db: RocksDB,
  private val cfHandle: ColumnFamilyHandle,
  private val keyCodec: Codec<K>,
  private val valueCodec: Codec<V>,
  private val writeQueue: RocksdbWriteQueue,
  private val disposable: Disposable,
) : SortedKVStore<K, V>, Disposable {

  companion object {
    private val READ_OPTIONS = ReadOptions()
    private val WRITE_OPTIONS = WriteOptions()
  }

  private val cache = Caffeine.newBuilder()
    .weakKeys()
    .weakValues()
    .build<K, V?>()

  private val writeOperationDesc: WriteOperationDesc<K, V> = WriteOperationDesc(db, cfHandle, keyCodec, valueCodec)

  private val maxReference: AtomicReference<K> = AtomicReference()

  init {
    Disposer.register(disposable, this)
  }

  override fun getHighestKey(): K? {
    TODO("Not yet implemented")
  }

  override fun get(key: K): V? = cache.get(key) {
    val keyBuffer = UnsafeRocksdbCodecBuffer.allocate()
    keyCodec.encode(UnsafeRocksdbCodecContext, keyBuffer, key)

    val valueBuffer = UnsafeRocksdbCodecBuffer.allocate(0)
    val result = db.get(cfHandle, READ_OPTIONS, keyBuffer.buffer, valueBuffer.buffer)
    if (result == RocksDB.NOT_FOUND) {
      null
    } else {
      valueCodec.decode(UnsafeRocksdbCodecContext, valueBuffer)
    }
  }

  override fun put(key: K, value: V) {
    cache.put(key, value)
    writeQueue.enqueue(WriteOperation.WritePlain(writeOperationDesc, key, value))
  }

  override fun contains(key: K): Boolean {
    if (cache.getIfPresent(key) != null) {
      return true
    }
    val keyBuffer = UnsafeRocksdbCodecBuffer.allocate()
    keyCodec.encode(UnsafeRocksdbCodecContext, keyBuffer, key)

    return db.keyExists(cfHandle, READ_OPTIONS, keyBuffer.buffer)
  }

  override fun remove(key: K, useReturn: Boolean): V? {
    val value = if (useReturn) {
      get(key)
    } else {
      null
    }
    cache.invalidate(key)
    val buffer = UnsafeRocksdbCodecBuffer.allocate()
    keyCodec.encode(UnsafeRocksdbCodecContext, buffer, key)
    db.delete(cfHandle, WRITE_OPTIONS, buffer.buffer)
    return value
  }

  override fun clear() {
    cache.invalidateAll()

    val first = db.newIterator().use {
      it.seekToFirst()
      if (it.isValid()) {
        it.key()
      } else {
        null
      }
    }
    val last = db.newIterator().use {
      it.seekToLast()
      if (it.isValid()) {
        it.key()
      } else {
        null
      }
    }
    if (first != null && last != null) {
      db.deleteRange(cfHandle, WRITE_OPTIONS, first, last)
    }
  }

  override fun keys(): Sequence<K> = sequence {
    db.newIterator(cfHandle).use { iterator ->
      iterator.seekToFirst()
      while (iterator.isValid) {
        val buffer = UnsafeRocksdbCodecBuffer.allocate(0)
        TODO("todo")
        //yield(iterator.key())
        iterator.next()
      }
    }
  }

  override fun values(): Sequence<V> {
    TODO("Not yet implemented")
  }

  override fun asSequence(): Sequence<Pair<K, V>> {

    TODO("Not yet implemented")
  }

  override fun computeIfAbsent(key: K, op: (k: K) -> V): V? {
    TODO("Not yet implemented")
  }

  override fun compute(key: K, op: (k: K, v: V?) -> V?): V? {
    TODO("Not yet implemented")
  }

  override fun dispose() {
    cfHandle.close()
  }
}
