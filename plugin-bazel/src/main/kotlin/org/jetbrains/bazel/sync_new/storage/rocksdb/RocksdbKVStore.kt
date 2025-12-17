package org.jetbrains.bazel.sync_new.storage.rocksdb

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.storage.BaseKVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.CloseableIterator
import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.mapCloseable
import org.jetbrains.bazel.sync_new.storage.util.UnsafeByteBufferCodecBuffer
import org.jetbrains.bazel.sync_new.storage.util.UnsafeCodecContext
import org.rocksdb.ColumnFamilyHandle
import org.rocksdb.ReadOptions
import org.rocksdb.RocksDB
import org.rocksdb.RocksIterator
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.StampedLock

class RocksdbKVStore<K : Any, V : Any>(
  private val db: RocksDB,
  internal val cfHandle: ColumnFamilyHandle,
  internal val keyCodec: Codec<K>,
  internal val valueCodec: Codec<V>,
  private val flushQueue: RocksdbFlushQueue,
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

  private val cache = Caffeine.newBuilder()
    .executor { it.run() }
    .evictionListener<K, V> { k, v, cause ->
      if (k == null || v == null) {
        return@evictionListener
      }
      when (cause) {
        RemovalCause.COLLECTED, RemovalCause.EXPIRED, RemovalCause.SIZE -> {
          evictedQueue[k] = v
          flushQueue.enqueue(FlushOperation.FlushDirty(this, k))
        }

        else -> {
          /* noop */
        }
      }
    }
    .expireAfterAccess(5, TimeUnit.MINUTES)
    .build<K, V?>()

  private val clearLock = Any()

  private val computeLocks: ConcurrentMap<K, StampedLock> = ConcurrentHashMap()
  private val evictedQueue: ConcurrentMap<K, V> = ConcurrentHashMap()

  init {
    Disposer.register(disposable, this)
  }

  override fun get(key: K): V? {
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
        keyBuffer.buffer.rewind()
      }

      VALUE_BUFFER_POOL.use { buffer ->
        buffer.buffer.clear()
        val size = db.get(cfHandle, RANDOM_READ_OPTIONS, keyBuffer.buffer, buffer.buffer)
        if (size == RocksDB.NOT_FOUND) {
          null
        } else {
          if (size > buffer.buffer.capacity()) {
            buffer.resizeTo(size)
            keyBuffer.buffer.rewind()
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
    putInternal(key, value, acquireLock = true)
  }

  override fun contains(key: K): Boolean {
    if (cache.getIfPresent(key) != null) {
      return true
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
    return removeInternal(key, useReturn = useReturn, acquireLock = true)
  }

  override fun clear() {
    synchronized(clearLock) {
      val latch = CountDownLatch(1)
      flushQueue.enqueue(FlushOperation.Barrier(this) { latch.countDown() })
      latch.await()
      cache.invalidateAll()
      computeLocks.clear()
      evictedQueue.clear()
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

  // TODO: remove duplicated code
  override fun keys(): CloseableIterator<K> = object : CloseableIterator<K> {
    private var iter: RocksIterator? = null
    private val yieldQueue = HashSet(cache.asMap().keys)

    override fun next(): K {
      val iter = iter
      if (iter != null && iter.isValid) {
        val key = keyCodec.decode(UnsafeCodecContext, UnsafeByteBufferCodecBuffer.from(iter.key()))
        yieldQueue.remove(key)
        iter.next()
        return key
      }

      while (yieldQueue.isNotEmpty()) {
        val key = yieldQueue.first()
        yieldQueue.remove(key)
        val value = evictedQueue.remove(key) ?: cache.getIfPresent(key)
        if (value != null) {
          return key
        }
      }

      throw NoSuchElementException()
    }

    override fun hasNext(): Boolean {
      val iter = if (iter == null) {
        val newIter = db.newIterator(cfHandle, SEQ_READ_OPTIONS)
        newIter.seekToFirst()
        if (!newIter.isValid) {
          newIter.close()
          return yieldQueue.isNotEmpty()
        }
        iter = newIter
        newIter
      } else {
        iter!!
      }
      return iter.isValid || yieldQueue.isNotEmpty()
    }

    override fun close() {
      iter?.close()
    }
  }

  override fun values(): CloseableIterator<V> = iterator().mapCloseable { (_, v) -> v }

  override fun iterator(): CloseableIterator<Pair<K, V>> = object : CloseableIterator<Pair<K, V>> {
    private var iter: RocksIterator? = null
    private val yieldQueue = HashSet(cache.asMap().keys)

    override fun next(): Pair<K, V> {
      val iter = iter
      if (iter != null && iter.isValid) {
        val key = keyCodec.decode(UnsafeCodecContext, UnsafeByteBufferCodecBuffer.from(iter.key()))
        yieldQueue.remove(key)
        val evicted = evictedQueue.remove(key)
        if (evicted != null) {
          iter.next()
          return key to evicted
        }
        val cached = cache.getIfPresent(key)
        val pair = if (cached == null) {
          val value = valueCodec.decode(UnsafeCodecContext, UnsafeByteBufferCodecBuffer.from(iter.value()))
          cache.put(key, value)
          key to value
        } else {
          key to cached
        }
        iter.next()
        return pair
      }

      while (yieldQueue.isNotEmpty()) {
        val key = yieldQueue.first()
        yieldQueue.remove(key)
        val value = evictedQueue.remove(key) ?: cache.getIfPresent(key)
        if (value != null) {
          return key to value
        }
      }

      throw NoSuchElementException()
    }

    override fun hasNext(): Boolean {
      val iter = if (iter == null) {
        val newIter = db.newIterator(cfHandle, SEQ_READ_OPTIONS)
        newIter.seekToFirst()
        if (!newIter.isValid) {
          newIter.close()
          return yieldQueue.isNotEmpty()
        }
        iter = newIter
        newIter
      } else {
        iter!!
      }
      return iter.isValid || yieldQueue.isNotEmpty()
    }

    override fun close() {
      iter?.close()
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
      putInternal(key, v, acquireLock = false)
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
        removeInternal(key, useReturn = false, acquireLock = false)
      } else {
        putInternal(key, newValue, acquireLock = false)
      }
      return@useComputeLock newValue
    } finally {
      lock.unlock(stamp)
    }
  }

  override fun dispose() {
    cfHandle.close()
  }

  private fun putInternal(key: K, value: V, acquireLock: Boolean) = useWriteLock(key, acquire = acquireLock) {
    cache.put(key, value)
    flushQueue.enqueue(FlushOperation.FlushDirty(this, key))
  }

  private fun removeInternal(key: K, useReturn: Boolean, acquireLock: Boolean) = useWriteLock(key, acquire = acquireLock) {
    val value = if (useReturn) {
      get(key)
    } else {
      null
    }
    evictedQueue.remove(key)
    cache.invalidate(key)
    flushQueue.enqueue(FlushOperation.FlushDirty(this, key))
    value
  }

  private inline fun <T> useComputeLock(key: K, crossinline op: (lock: StampedLock) -> T): T {
    val lock = computeLocks.computeIfAbsent(key) { StampedLock() }
    try {
      return op(lock)
    } finally {
      computeLocks.remove(key)
    }
  }

  private inline fun <T> useWriteLock(key: K, acquire: Boolean, op: () -> T): T {
    if (!acquire) {
      return op()
    }
    val lock = computeLocks[key]
    if (lock == null) {
      return op()
    }
    val stamp = lock.writeLock()
    try {
      return op()
    } finally {
      lock.unlock(stamp)
    }
  }

  internal fun consumeKeyState(key: K): KeyState<K, V> {
    val cachedValue = cache.getIfPresent(key)
    val evictedValue = evictedQueue.remove(key)
    return when {
      evictedValue != null && cachedValue == null -> KeyState.Evicted(evictedValue)
      cachedValue == null -> KeyState.Removed(key)
      else -> KeyState.Added(cachedValue)
    }
  }
}

sealed interface KeyState<K, V> {
  data class Added<K, V>(val value: V) : KeyState<K, V>
  data class Removed<K, V>(val key: K) : KeyState<K, V>
  data class Evicted<K, V>(val value: V) : KeyState<K, V>
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
      flushQueue = owner.flushQueue,
      disposable = disposable,
    )
  }

}
