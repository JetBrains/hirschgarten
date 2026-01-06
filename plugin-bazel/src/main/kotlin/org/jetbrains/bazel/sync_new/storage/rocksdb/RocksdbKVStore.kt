package org.jetbrains.bazel.sync_new.storage.rocksdb

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.RemovalCause
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.locks.StampedLock

class RocksdbKVStore<K : Any, V : Any>(
  private val db: RocksDB,
  internal val cfHandle: ColumnFamilyHandle,
  internal val keyCodec: Codec<K>,
  internal val valueCodec: Codec<V>,
  private val flushQueue: RocksdbFlushQueue,
) : KVStore<K, V> {

  companion object {
    private const val USE_BLOOM_EXISTENCE_CHECK = true

    // optimized for point lookups with bloom filter support
    private val RANDOM_READ_OPTIONS = ReadOptions()
      .setVerifyChecksums(false)
      .setFillCache(true)
      .setReadaheadSize(0)

    // optimized for sequential scans with larger readahead
    private val SEQ_READ_OPTIONS = ReadOptions()
      .setVerifyChecksums(false)
      .setFillCache(false)
      .setReadaheadSize(4 * 1024 * 1024)
      .setTailing(false)

    private val MIN_KEY = ByteArray(0)
    private val MAX_KEY = ByteArray(256) { 0xFF.toByte() }
  }

  private sealed interface Value<out V> {
    data class Present<V>(val value: V) : Value<V>
    data object Tombstone : Value<Nothing>
  }

  private sealed interface Dirty<out V> {
    data class Put<V>(val value: V) : Dirty<V>
    data object Delete : Dirty<Nothing>
  }

  private val cache: Cache<K, Value<V>> = Caffeine.newBuilder()
    .executor { it.run() }
    .removalListener<K, Value<V>> { key, value, cause ->
      when (cause) {
        RemovalCause.EXPIRED, RemovalCause.SIZE -> {
          when (value) {
            is Value.Present<V> -> dirty[key] = Dirty.Put<V>(value.value)
            Value.Tombstone -> dirty[key] = Dirty.Delete
            null -> {
              /* noop */
            }
          }
        }

        else -> {}
      }
    }
    .build()

  private val computeLocks: ConcurrentMap<K, StampedLock> = ConcurrentHashMap()
  private val dirty: ConcurrentMap<K, Dirty<V>> = ConcurrentHashMap()

  override fun get(key: K): V? {
    // value hasn't been flushed yet
    when (val dirty = dirty[key]) {
      Dirty.Delete -> return null
      is Dirty.Put<V> -> return dirty.value
      null -> {
        /* noop */
      }
    }

    // value is cached
    when (val cached = cache.getIfPresent(key)) {
      is Value.Present<V> -> return cached.value
      Value.Tombstone -> return null
      null -> {}
    }

    // load from rocksdb
    val value = loadFromRocksdb(key)
    if (value == null) {
      cache.put(key, Value.Tombstone)
    } else {
      cache.put(key, Value.Present(value))
    }
    return value
  }

  private fun loadFromRocksdb(key: K): V? = UnsafeByteBufferObjectPool.use { keyBuf, valueBuf ->
    keyCodec.encode(UnsafeCodecContext, keyBuf, key)
    keyBuf.flip()

    if (USE_BLOOM_EXISTENCE_CHECK) {
      val mayExist = db.keyMayExist(cfHandle, RANDOM_READ_OPTIONS, keyBuf.buffer)
      if (!mayExist) {
        return@use null
      }
      keyBuf.buffer.rewind()
    }

    val size = db.get(cfHandle, RANDOM_READ_OPTIONS, keyBuf.buffer, valueBuf.buffer)
    if (size == RocksDB.NOT_FOUND) {
      null
    }
    else {
      if (size > valueBuf.buffer.capacity()) {
        valueBuf.resizeTo(size)
        valueBuf.buffer.clear()
        keyBuf.buffer.rewind()
        db.get(cfHandle, RANDOM_READ_OPTIONS, keyBuf.buffer, valueBuf.buffer)
      }
      valueCodec.decode(UnsafeCodecContext, valueBuf)
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
    dirty.clear()
    WriteBatch().use { batch ->
      batch.deleteRange(cfHandle, MIN_KEY, MAX_KEY)
      WriteOptions().use { opts -> db.write(opts, batch) }
    }
  }

  override fun keys(): CloseableIterator<K> = createIter(useValues = false) { k, _ -> k }

  override fun values(): CloseableIterator<V> = createIter(useValues = true) { _, v -> v!! }

  override fun iterator(): CloseableIterator<Pair<K, V>> = createIter(useValues = true) { k, v -> Pair(k, v!!) }

  private fun <T> createIter(
    useValues: Boolean,
    transform: (k: K, v: V?) -> T,
  ): CloseableIterator<T> = object : CloseableIterator<T> {

    // snapshot of dirty overlay at iterator creation time
    // this allows to keep copy-on-write like semantics
    private val dirtySnapshot: Map<K, Dirty<V>> = HashMap(dirty)
    private val dirtyKeysIter: Iterator<Map.Entry<K, Dirty<V>>> = dirtySnapshot.entries.iterator()

    private val yielded = HashSet<K>() // keys already returned
    private var dbIter: RocksIterator? = null
    private var nextEntry: Pair<K, V?>? = null

    override fun hasNext(): Boolean {
      if (nextEntry != null) return true
      return advance()
    }

    override fun next(): T {
      if (nextEntry == null && !advance()) {
        throw NoSuchElementException()
      }
      val (k, v) = nextEntry!!
      nextEntry = null
      return transform(k, v)
    }

    override fun close() {
      dbIter?.close()
    }

    // incremental iterator implementation
    private fun advance(): Boolean {
      // yield first, not-yielded dirty put
      while (dirtyKeysIter.hasNext()) {
        val (k, d) = dirtyKeysIter.next()
        if (!yielded.add(k)) continue

        when (d) {
          is Dirty.Put<V> -> {
            // most recent dirty value
            val v = if (useValues) {
              d.value
            } else {
              null
            }
            nextEntry = Pair(k, v)
            return true
          }

          Dirty.Delete -> {
            // skip removed
            continue
          }
        }
      }

      // overlay cache with rocksdb persistance layer
      if (dbIter == null) {
        val it = db.newIterator(cfHandle, SEQ_READ_OPTIONS)
        it.seekToFirst()
        dbIter = it
      }

      val it = dbIter!!
      while (it.isValid) {
        val keyBuf = UnsafeByteBufferCodecBuffer.from(it.key())
        val k = keyCodec.decode(UnsafeCodecContext, keyBuf)

        // skip more recent yielded key from key
        if (!yielded.add(k)) {
          it.next()
          continue
        }

        // check dirty overlay snapshot first
        val dirtyState = dirtySnapshot[k]
        when (dirtyState) {
          is Dirty.Put<V> -> {
            // Pending put overrides what's on disk
            nextEntry = k to if (useValues) dirtyState.value else null
            it.next()
            return true
          }

          Dirty.Delete -> {
            // skip deleted
            it.next()
            continue
          }

          null -> {
            // no pending change, consult cache tombstones to avoid resurrecting
            when (val cached = cache.getIfPresent(k)) {
              is Value.Tombstone -> {
                // skip delete
                it.next()
                continue
              }

              is Value.Present<V> -> {
                val v = if (useValues) {
                  cached.value
                }
                else {
                  null
                }
                nextEntry = Pair(k, v)
                it.next()
                return true
              }

              null -> {
                // both dirty and cache are not containing value
                // that mean value was never loaded into the cache or was evicted in the past
                val v: V? = if (useValues) {
                  val valBuf = UnsafeByteBufferCodecBuffer.from(it.value())
                  valueCodec.decode(UnsafeCodecContext, valBuf)
                }
                else null

                if (useValues && v != null) {
                  // cache value for faster future iterations
                  cache.put(k, Value.Present(v))
                }

                nextEntry = k to v
                it.next()
                return true
              }
            }
          }
        }
      }

      return false
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
      }
      else {
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
      }
      else {
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

  private fun putInternal(key: K, value: V, acquireLock: Boolean) = useWriteLock(key, acquire = acquireLock) {
    cache.put(key, Value.Present(value))
    dirty[key] = Dirty.Put(value)
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
    dirty[key] = Dirty.Delete
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
    val state = dirty.remove(key) ?: return KeyAction.Noop()
    return when (state) {
      Dirty.Delete -> KeyAction.Remove(key)
      is Dirty.Put<V> -> KeyAction.Overwrite(state.value)
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
) : BaseKVStoreBuilder<RocksdbKVStoreBuilder<K, V>, K, V>() {
  override fun build(): KVStore<K, V> {
    return RocksdbKVStore(
      db = owner.db,
      cfHandle = owner.createColumnFamily(name),
      keyCodec = keyCodec ?: error("Key codec must be specified"),
      valueCodec = valueCodec ?: error("Value codec must be specified"),
      flushQueue = owner.flushQueue,
    )
  }

}
