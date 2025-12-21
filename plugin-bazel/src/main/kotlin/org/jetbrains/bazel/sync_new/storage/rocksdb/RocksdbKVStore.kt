package org.jetbrains.bazel.sync_new.storage.rocksdb

import com.github.benmanes.caffeine.cache.Caffeine
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.jetbrains.bazel.sync_new.codec.Codec
import org.jetbrains.bazel.sync_new.storage.BaseKVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.CloseableIterator
import org.jetbrains.bazel.sync_new.storage.KVStore
import org.jetbrains.bazel.sync_new.storage.util.UnsafeByteBufferCodecBuffer
import org.jetbrains.bazel.sync_new.storage.util.UnsafeByteBufferObjectPool
import org.jetbrains.bazel.sync_new.storage.util.UnsafeCodecContext
import org.rocksdb.ColumnFamilyHandle
import org.rocksdb.ReadOptions
import org.rocksdb.RocksDB
import org.rocksdb.RocksIterator
import org.rocksdb.WriteBatch
import org.rocksdb.WriteOptions
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.StampedLock

// TODO: rewrite this
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

    private val MIN_KEY = ByteArray(0)
    private val MAX_KEY = ByteArray(256) { 0xFF.toByte() }
  }

  private sealed interface Value<out V> {
    data class Present<V>(val value: V) : Value<V>
    data object Tombstone : Value<Nothing>
  }

  private val cache = Caffeine.newBuilder()
    .executor { it.run() }
    .build<K, Value<V>>()

  private val computeLocks: ConcurrentMap<K, StampedLock> = ConcurrentHashMap()
  private val removalQueue: ConcurrentSkipListSet<K> = ConcurrentSkipListSet()

  init {
    Disposer.register(disposable, this)
  }

  override fun get(key: K): V? {
    val value = cache.get(key) { k ->
      UnsafeByteBufferObjectPool.use { keyBuf, valueBuf ->
        keyCodec.encode(UnsafeCodecContext, keyBuf, k)
        keyBuf.flip()

        if (USE_BLOOM_EXISTENCE_CHECK) {
          val mayExist = db.keyMayExist(cfHandle, RANDOM_READ_OPTIONS, keyBuf.buffer)
          if (!mayExist) {
            return@use Value.Tombstone
          }
          keyBuf.buffer.rewind()
        }

        val size = db.get(cfHandle, RANDOM_READ_OPTIONS, keyBuf.buffer, valueBuf.buffer)
        if (size == RocksDB.NOT_FOUND) {
          Value.Tombstone
        }
        else {
          if (size > valueBuf.buffer.capacity()) {
            valueBuf.resizeTo(size)
            valueBuf.buffer.clear()
            keyBuf.buffer.rewind()
            db.get(cfHandle, RANDOM_READ_OPTIONS, keyBuf.buffer, valueBuf.buffer)
          }
          Value.Present(valueCodec.decode(UnsafeCodecContext, valueBuf))
        }
      }
    }
    return when (value) {
      is Value.Present -> value.value
      Value.Tombstone -> null
    }
  }

  override fun put(key: K, value: V) {
    putInternal(key, value, acquireLock = true)
  }

  override fun contains(key: K): Boolean {
    val cached = cache.getIfPresent(key)
    if (cached != null) {
      return when (cached) {
        is Value.Present<V> -> true
        Value.Tombstone -> false
      }
    }

    return UnsafeByteBufferObjectPool.use { buffer ->
      keyCodec.encode(UnsafeCodecContext, buffer, key)
      buffer.flip()

      if (USE_BLOOM_EXISTENCE_CHECK) {
        val mayExist = db.keyMayExist(cfHandle, RANDOM_READ_OPTIONS, buffer.buffer)
        if (!mayExist) {
          return@use false
        }
      }

      db.keyExists(cfHandle, RANDOM_READ_OPTIONS, buffer.buffer)
    }
  }

  override fun remove(key: K, useReturn: Boolean): V? {
    return removeInternal(key, useReturn = useReturn, acquireLock = true)
  }

  override fun clear() {
    val latch = CountDownLatch(1)
    flushQueue.enqueue(FlushOperation.Barrier(this) { latch.countDown() })
    latch.await()

    cache.invalidateAll()
    computeLocks.clear()
    WriteBatch().use { batch ->
      batch.deleteRange(cfHandle, MIN_KEY, MAX_KEY)
      WriteOptions().use { opts -> db.write(opts, batch) }
    }
  }

  override fun keys(): CloseableIterator<K> = createIter(useValues = false) { k, _ -> k }

  override fun values(): CloseableIterator<V> = createIter(useValues = true) { _, v -> v!! }

  override fun iterator(): CloseableIterator<Pair<K, V>> = createIter(useValues = true) { k, v -> Pair(k, v!!) }

  private fun <T> createIter(useValues: Boolean, transform: (k: K, v: V?) -> T): CloseableIterator<T> = object : CloseableIterator<T> {
    private val cacheQueue = LinkedHashSet<K>(cache.asMap().keys)
    private val yielded = HashSet<K>()
    private var nextKey: K? = null
    private var iter: RocksIterator? = null

    override fun next(): T {
      if (cacheQueue.isNotEmpty()) {
        val key = cacheQueue.removeFirst()
        val cached = cache.getIfPresent(key)
        if (cached != null) {
          when (cached) {
            is Value.Present<V> -> {
              yielded.add(key)
              return transform(key, cached.value)
            }
            Value.Tombstone -> {
              // concurrent modification
              // TODO: handle this case separately or make iterator copy-on-write
            }
          }
        }
      }

      val nextKey = nextKey
      val iter = iter
      if (iter != null && nextKey != null && iter.isValid) {
        val value = if (useValues) {
          // TODO: use more performant unsafe buffer
          val buffer = UnsafeByteBufferCodecBuffer.from(iter.value())
          valueCodec.decode(UnsafeCodecContext, buffer)
        }
        else {
          null
        }
        iter.next()
        yielded.add(nextKey)
        this.nextKey = null
        return transform(nextKey, value)
      }

      throw NoSuchElementException()
    }

    override fun hasNext(): Boolean {
      if (iter == null) {
        val newIter = db.newIterator(cfHandle, SEQ_READ_OPTIONS)
        newIter.seekToFirst()
        iter = newIter
      }
      while (cacheQueue.isNotEmpty()) {
        val key = cacheQueue.first
        if (yielded.contains(key)) {
          continue
        }
        val cached = cache.getIfPresent(key)
        if (cached != null) {
          when (cached) {
            is Value.Present<V> -> return true
            Value.Tombstone -> cacheQueue.remove(key)
          }
        }
      }
      val iter = iter
      if (iter != null) {
        while (iter.isValid) {
          val buffer = UnsafeByteBufferCodecBuffer.from(iter.key())
          val key = keyCodec.decode(UnsafeCodecContext, buffer)
          if (yielded.contains(key)) {
            iter.next()
            continue
          }
          nextKey = key
          if (!cacheQueue.contains(key)) {
            return true
          }
          iter.next()
        }
      }
      return false
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
      val newStamp = lock.tryConvertToWriteLock(stamp)
      if (newStamp == 0L) {
        lock.unlockRead(stamp)
        stamp = lock.writeLock()
      } else {
        stamp = newStamp
      }
      v = get(key)
      if (v != null) {
        return@useComputeLock v
      }
      v = op(key)
      putInternal(key, v, acquireLock = false)
      return@useComputeLock v
    }
    finally {
      lock.unlock(stamp)
    }
  }

  override fun compute(key: K, op: (k: K, v: V?) -> V?): V? = useComputeLock(key) { lock ->
    var stamp = lock.readLock()
    try {
      // keep read lock in case if op() is expensive
      val v = get(key)
      val newValue = op(key, v)
      val newStamp = lock.tryConvertToWriteLock(stamp)
      if (newStamp == 0L) {
        lock.unlockRead(stamp)
        stamp = lock.writeLock()
      } else {
        stamp = newStamp
      }
      if (newValue == null) {
        removeInternal(key, useReturn = false, acquireLock = false)
      }
      else {
        putInternal(key, newValue, acquireLock = false)
      }
      return@useComputeLock newValue
    }
    finally {
      lock.unlock(stamp)
    }
  }

  override fun dispose() {
    cfHandle.close()
  }

  private fun putInternal(key: K, value: V, acquireLock: Boolean) = useWriteLock(key, acquire = acquireLock) {
    cache.put(key, Value.Present(value))
    flushQueue.enqueue(FlushOperation.FlushDirty(this, key))
  }

  private fun removeInternal(key: K, useReturn: Boolean, acquireLock: Boolean) = useWriteLock(key, acquire = acquireLock) {
    val value = if (useReturn) {
      get(key)
    }
    else {
      null
    }
    cache.put(key, Value.Tombstone)
    flushQueue.enqueue(FlushOperation.FlushDirty(this, key))
    value
  }

  private inline fun <T> useComputeLock(key: K, crossinline op: (lock: StampedLock) -> T): T {
    val lock = computeLocks.computeIfAbsent(key) { StampedLock() }
    try {
      return op(lock)
    }
    finally {
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
    }
    finally {
      lock.unlock(stamp)
    }
  }

  internal fun consumeKey(key: K): KeyAction<K, V> {
    if (removalQueue.remove(key)) {
      // TODO: handle cache evictions here
      return KeyAction.Remove(key)
    }
    val value = cache.getIfPresent(key)
    if (value == null) {
      // states:
      // 1. not loaded yet
      // 2. removed
      // 3. TODO: evicted - look at this case more closely
      return KeyAction.Noop()
    }
    return when (value) {
      is Value.Present<V> -> KeyAction.Overwrite(value.value)
      Value.Tombstone -> KeyAction.Remove(key)
    }
  }
}

sealed interface KeyAction<K, V> {
  data class Overwrite<K, V>(val value: V) : KeyAction<K, V>
  data class Remove<K, V>(val key: K) : KeyAction<K, V>
  class Noop<K, V> : KeyAction<K, V>
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
