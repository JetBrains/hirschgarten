package org.jetbrains.bazel.sync_new.storage.rocksdb

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.storage.BaseKVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.util.UnsafeByteBufferCodecBuffer
import org.jetbrains.bazel.sync_new.storage.util.UnsafeCodecContext
import org.rocksdb.ColumnFamilyHandle
import org.rocksdb.ReadOptions
import org.rocksdb.RocksDB
import org.rocksdb.WriteBatch
import org.rocksdb.WriteBatchWithIndex
import org.rocksdb.WriteOptions
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.StampedLock

// TODO: handle queue offer timeouts properly, retries etc
class RocksdbKVStore<K : Any, V : Any>(
  private val db: RocksDB,
  private val cfHandle: ColumnFamilyHandle,
  private val keyCodec: Codec<K>,
  private val valueCodec: Codec<V>,
  private val writeQueue: RocksdbWriteQueue,
  private val disposable: Disposable,
) : KVStore<K, V>, Disposable {

  companion object {
    private const val USE_BLOOM_EXISTENCE_CHECK = true

    // optimized for point lookups with bloom filter support
    private val RANDOM_READ_OPTIONS = ReadOptions()
      .setVerifyChecksums(false)
      .setFillCache(true)
      .setReadaheadSize(0)
      .setIgnoreRangeDeletions(true)

    // optimized for sequential scans with larger readahead
    private val SEQ_READ_OPTIONS = ReadOptions()
      .setVerifyChecksums(false)
      .setFillCache(false)
      .setReadaheadSize(4 * 1024 * 1024)
      .setIgnoreRangeDeletions(true)
      .setTailing(false)

    private val KEY_BUFFER_POOL = RocksdbThreadLocalBufferPool(initialSize = 128)
    private val VALUE_BUFFER_POOL = RocksdbThreadLocalBufferPool(initialSize = 4096)
  }

  private data class PendingOp<V>(
    val kind: PendingOpKind,
    val value: V?,
  )

  private enum class PendingOpKind {
    WRITE, DELETE
  }

  private val cache = Caffeine.newBuilder()
    .softValues()
    .executor { it.run() }
    .build<K, V?>()

  private val clearLock = Any()

  private val writeOpDesc: WriteOperationDesc<K, V> = WriteOperationDesc(db, cfHandle, keyCodec, valueCodec)
  private val pendingOps: ConcurrentMap<K, PendingOp<V>> = ConcurrentHashMap()
  private val computeLocks: ConcurrentMap<K, StampedLock> = ConcurrentHashMap()

  init {
    Disposer.register(disposable, this)
  }

  override fun get(key: K): V? {
    val pending = pendingOps[key]
    if (pending != null) {
      return when (pending.kind) {
        PendingOpKind.WRITE -> pending.value!!
        PendingOpKind.DELETE -> null
      }
    }
    return cache.get(key) { k ->
      val keyBuffer = KEY_BUFFER_POOL.use { buffer ->
        keyCodec.encode(UnsafeCodecContext, buffer, k)
        buffer.flip()
        buffer
      }

      if (USE_BLOOM_EXISTENCE_CHECK) {
        val mayExist = db.keyMayExist(cfHandle, RANDOM_READ_OPTIONS, keyBuffer.buffer)
        if (!mayExist) {
          return@get null
        }
      }

      VALUE_BUFFER_POOL.use { buffer ->
        val size = db.get(cfHandle, RANDOM_READ_OPTIONS, keyBuffer.buffer, buffer.buffer)
        if (size == RocksDB.NOT_FOUND) {
          null
        } else {
          if (size > buffer.size) {
            buffer.ensureCapacity(size)
            db.get(cfHandle, RANDOM_READ_OPTIONS, keyBuffer.buffer, buffer.buffer)
          }
          buffer.buffer.position(0)
          buffer.buffer.limit(size)
          valueCodec.decode(UnsafeCodecContext, buffer)
        }
      }
    }
  }

  override fun put(key: K, value: V) {
    cache.put(key, value)
    pendingOps[key] = PendingOp(PendingOpKind.WRITE, value)
    val operation = WriteOperation.WritePlain(
      desc = writeOpDesc,
      key = key,
      value = value,
      callback = { k, _ ->
        pendingOps.compute(k) { _, v ->
          if (v == null) {
            null
          } else {
            if (v.kind == PendingOpKind.WRITE && v.value == null) {
              null
            } else {
              v
            }
          }
        }
      },
    )
    this.writeQueue.enqueue(operation)
  }

  override fun contains(key: K): Boolean {
    if (cache.getIfPresent(key) != null) {
      return true
    }

    val pending = pendingOps[key]
    if (pending != null) {
      return when (pending.kind) {
        PendingOpKind.WRITE -> true
        PendingOpKind.DELETE -> false
      }
    }

    val keyBuffer = KEY_BUFFER_POOL.use { buffer ->
      keyCodec.encode(UnsafeCodecContext, buffer, key)
      buffer.flip()
      buffer
    }

    if (USE_BLOOM_EXISTENCE_CHECK) {
      val mayExist = db.keyMayExist(cfHandle, RANDOM_READ_OPTIONS, keyBuffer.buffer)
      if (!mayExist) {
        return false
      }
    }

    return db.keyExists(cfHandle, RANDOM_READ_OPTIONS, keyBuffer.buffer)
  }

  override fun remove(key: K, useReturn: Boolean): V? {
    val value = if (useReturn) {
      get(key)
    } else {
      null
    }
    cache.invalidate(key)
    pendingOps[key] = PendingOp(PendingOpKind.DELETE, value)
    val operation = WriteOperation.RemovePlain(
      desc = writeOpDesc,
      key = key,
      callback = { k ->
        pendingOps.compute(k) { _, v ->
          if (v == null) {
            null
          } else {
            if (v.kind == PendingOpKind.DELETE) {
              null
            } else {
              v
            }
          }
        }
      },
    )
    this.writeQueue.enqueue(operation)
    return value
  }

  override fun clear() {
    synchronized(clearLock) {
      val latch = CountDownLatch(1)
      writeQueue.enqueue(WriteOperation.Barrier { latch.countDown() })
      latch.await()
      cache.invalidateAll()
      computeLocks.clear()
      pendingOps.clear()
      val (start, end) = db.newIterator(cfHandle).use { iter ->
        iter.seekToFirst()
        if (iter.isValid) {
          val start = iter.key()
          iter.seekToLast()
          val end = iter.key()
          Pair(start, end)
        } else {
          null
        }
      } ?: return@synchronized
      db.deleteRange(cfHandle, start, end)
      db.delete(cfHandle, end)
      db.compactRange(cfHandle)
    }
  }

  // TODO: implement asSequence only for keys if needed
  override fun keys(): Sequence<K> = asSequence().map { it.first }

  override fun values(): Sequence<V> = asSequence().map { it.second }

  override fun asSequence(): Sequence<Pair<K, V>> = sequence {
    val yielded = mutableSetOf<K>()
    db.newIterator(cfHandle, SEQ_READ_OPTIONS).use { iter ->
      iter.seekToFirst()
      while (iter.isValid) {
        val keyBuffer = UnsafeByteBufferCodecBuffer(ByteBuffer.wrap(iter.key()))
        val key = keyCodec.decode(UnsafeCodecContext, keyBuffer)

        if (pendingOps[key] == null) {
          yielded += key

          val existingValue = cache.getIfPresent(key)
          if (existingValue == null) {
            val valueBuffer = UnsafeByteBufferCodecBuffer(ByteBuffer.wrap(iter.value()))
            val value = valueCodec.decode(UnsafeCodecContext, valueBuffer)
            cache.put(key, value)
            yield(key to value)
          } else {
            yield(key to existingValue)
          }
        }
        iter.next()
      }
    }
    for ((key, op) in pendingOps) {
      if (yielded.contains(key)) {
        continue
      }
      when (op.kind) {
        PendingOpKind.WRITE -> yield(Pair(key, op.value!!))
        PendingOpKind.DELETE -> { /* noop */
        }
      }
    }
  }


  override fun computeIfAbsent(key: K, op: (k: K) -> V): V? = useComputeLock(key) {
    val lock = computeLocks.computeIfAbsent(key) { StampedLock() }
    var stamp = lock.readLock()
    try {
      var v = get(key)
      if (v != null) {
        return@useComputeLock v
      }
      stamp = lock.tryConvertToWriteLock(stamp)
      if (stamp == 0L) {
        lock.unlockRead(stamp)
        stamp = lock.writeLock()
      }
      v = get(key)
      if (v != null) {
        return@useComputeLock v
      }
      v = op(key)
      put(key, v)
      return@useComputeLock v
    } finally {
      lock.unlock(stamp)
    }
  }

  override fun compute(key: K, op: (k: K, v: V?) -> V?): V? = useComputeLock(key) { lock ->
    var stamp = lock.readLock()
    try {
      // keep read lock in case if op() is expensive
      val v = get(key)
      val newValue = op(key, v)
      stamp = lock.tryConvertToWriteLock(stamp)
      if (stamp == 0L) {
        lock.unlockRead(stamp)
        stamp = lock.writeLock()
      }
      if (newValue == null) {
        remove(key, useReturn = false)
      } else {
        put(key, newValue)
      }
      return@useComputeLock newValue
    } finally {
      lock.unlock(stamp)
    }
  }

  override fun dispose() {
    cfHandle.close()
  }

  private inline fun <T> useComputeLock(key: K, crossinline op: (lock: StampedLock) -> T): T {
    val lock = computeLocks.computeIfAbsent(key) { StampedLock() }
    try {
      return op(lock)
    } finally {
      computeLocks.remove(key)
    }
  }
}

class RocksdbKVStoreBuilder<K : Any, V : Any>(
  private val owner: RocksdbStorageContext,
  private val name: String,
  private val disposable: Disposable,
) : BaseKVStoreBuilder<RocksdbKVStoreBuilder<K, V>, K, V>() {
  override fun build(): KVStore<K, V> {
    return RocksdbKVStore(
      db = owner.db,
      cfHandle = owner.createColumnFamily(name),
      keyCodec = keyCodec ?: error("Key codec must be specified"),
      valueCodec = valueCodec ?: error("Value codec must be specified"),
      writeQueue = owner.writeQueue,
      disposable = disposable,
    )
  }

}
