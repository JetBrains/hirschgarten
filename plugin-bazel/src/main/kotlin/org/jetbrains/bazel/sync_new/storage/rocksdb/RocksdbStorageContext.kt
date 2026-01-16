package org.jetbrains.bazel.sync_new.storage.rocksdb

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.openapi.util.Disposer
import org.jetbrains.bazel.sync_new.storage.FlatPersistentStore
import org.jetbrains.bazel.sync_new.storage.FlatStoreBuilder
import org.jetbrains.bazel.sync_new.storage.KVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.LifecycleStoreContext
import org.jetbrains.bazel.sync_new.storage.PersistentStoreOwner
import org.jetbrains.bazel.sync_new.storage.PersistentStoreWithModificationMarker
import org.jetbrains.bazel.sync_new.storage.StorageContext
import org.jetbrains.bazel.sync_new.storage.DefaultStorageHints
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.in_memory.InMemoryFlatStoreBuilder
import org.jetbrains.bazel.sync_new.storage.in_memory.InMemoryKVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.util.UnsafeByteBufferCodecBuffer
import org.jetbrains.bazel.sync_new.storage.util.UnsafeCodecContext
import org.rocksdb.AbstractComparator
import org.rocksdb.BlockBasedTableConfig
import org.rocksdb.BloomFilter
import org.rocksdb.BuiltinComparator
import org.rocksdb.ColumnFamilyDescriptor
import org.rocksdb.ColumnFamilyHandle
import org.rocksdb.ColumnFamilyOptions
import org.rocksdb.CompactionStyle
import org.rocksdb.ComparatorOptions
import org.rocksdb.CompressionType
import org.rocksdb.DBOptions
import org.rocksdb.FlushOptions
import org.rocksdb.IndexType
import org.rocksdb.LRUCache
import org.rocksdb.Options
import org.rocksdb.ReadOptions
import org.rocksdb.ReusedSynchronisationType
import org.rocksdb.RocksDB
import org.rocksdb.WriteOptions
import java.nio.ByteBuffer
import java.nio.file.Files
import kotlin.io.path.absolutePathString

class RocksdbStorageContext(
  internal val project: Project,
  private val disposable: Disposable,
) : StorageContext, LifecycleStoreContext, PersistentStoreOwner, Disposable {

  companion object {
    init {
      RocksDB.loadLibrary()
      System.setProperty("rocksdb.stats", "true")
    }

    // Optimized for point lookups (graph vertex/edge access)
    private val READ_OPTIONS = ReadOptions()
      .setVerifyChecksums(false)
      .setFillCache(true) // Enable block cache for hot data
      .setReadaheadSize(0) // No readahead for random access

    // Optimized for sequential scans (graph traversal)
    private val SCAN_READ_OPTIONS = ReadOptions()
      .setVerifyChecksums(false)
      .setFillCache(false) // Don't pollute cache during scans
      .setReadaheadSize(2 * 1024 * 1024) // 2MB readahead for sequential access

    private val WRITE_OPTIONS = WriteOptions()
  }

  private class FlatStorageHandler(
    val key: ByteArray,
  )

  private val lock: Any = Any()
  private val columnFamilyCache: MutableMap<String, ColumnFamilyHandle> = mutableMapOf()
  internal val db: RocksDB
  internal val flushQueue: RocksdbFlushQueue
  private val flatStorages: MutableMap<FlatPersistentStore, FlatStorageHandler> = mutableMapOf()
  private val inMemoryColumn: ColumnFamilyHandle

  init {
    Disposer.register(disposable, this)
    val options = Options()
      .setCreateIfMissing(true)
      .setCreateMissingColumnFamilies(true)
      // Write settings (balanced for graph ingestion)
      .setWriteBufferSize(64 * 1024 * 1024) // 64MB memtable
      .setMaxWriteBufferNumber(4)
      .setMinWriteBufferNumberToMerge(1)
      .setAllowConcurrentMemtableWrite(true)
      .setEnablePipelinedWrite(true)
      // Compaction settings (optimized for reads)
      .setMaxBackgroundJobs(16) // More jobs for better read latency
      .setMaxSubcompactions(8)
      .setCompactionStyle(CompactionStyle.LEVEL)
      .setTargetFileSizeBase(256 * 1024 * 1024)
      .setMaxBytesForLevelBase(1024 * 1024 * 1024) // 1GB L1 for better read performance
      .setMaxBytesForLevelMultiplier(8.0)
      .setLevel0FileNumCompactionTrigger(4) // Faster compaction for better reads
      .setLevel0SlowdownWritesTrigger(12)
      .setLevel0StopWritesTrigger(20)
      .setCompressionType(CompressionType.NO_COMPRESSION)
      // Read optimization settings
      .setMaxOpenFiles(-1) // Keep all files open for faster reads
      .setAllowMmapReads(true) // Enable mmap for faster reads
      .setAllowMmapWrites(false)
      .setAvoidUnnecessaryBlockingIO(true)
      .setUseDirectReads(false) // Use page cache for better performance
      .setUseDirectIoForFlushAndCompaction(false)
      // WAL settings (disabled for write performance)
      .setMaxTotalWalSize(0)
      .setWalTtlSeconds(0)
      .setWalSizeLimitMB(0)
      // Table options for reads
      .setTableFormatConfig(
        BlockBasedTableConfig()
          .setBlockCache(LRUCache(128 * 1024 * 1024, 8, false))
          .setBlockSize(32 * 1024) // 32KB blocks
          .setCacheIndexAndFilterBlocks(true)
          .setCacheIndexAndFilterBlocksWithHighPriority(true)
          .setPinL0FilterAndIndexBlocksInCache(true)
          .setPinTopLevelIndexAndFilter(true)
          .setFilterPolicy(BloomFilter(10.0, false)) // 10 bits per key
          .setWholeKeyFiltering(true)
          .setFormatVersion(5)
          .setIndexType(IndexType.kTwoLevelIndexSearch)
          .setPartitionFilters(true)
          .setOptimizeFiltersForMemory(false) // Optimize for speed, not memory
          .setMetadataBlockSize(8 * 1024),
      )

    val dbPath = project.getProjectDataPath("bazel-rocksdb.db")
    Files.createDirectories(dbPath.parent)
    val familyNames = RocksDB.listColumnFamilies(options, dbPath.absolutePathString()).ifEmpty { listOf(RocksDB.DEFAULT_COLUMN_FAMILY) }
    val cfDescriptors = familyNames.map { ColumnFamilyDescriptor(it, ColumnFamilyOptions()) }
    val cfHandles = mutableListOf<ColumnFamilyHandle>()
    db = RocksDB.open(DBOptions(options), dbPath.absolutePathString(), cfDescriptors, cfHandles)
    for (handle in cfHandles) {
      columnFamilyCache[handle.name.decodeToString()] = handle
    }
    flushQueue = RocksdbFlushQueue(db = db)
    inMemoryColumn = createColumnFamily("IN_MEMORY_STORE")
  }

  internal fun createColumnFamily(name: String, comparator: Comparator<ByteBuffer>? = null): ColumnFamilyHandle = synchronized(lock) {
    val column = columnFamilyCache[name]
    if (column != null) {
      return@synchronized column
    }

    // Shared block cache across all column families for better memory utilization
    val sharedBlockCache = LRUCache(128 * 1024 * 1024) // 128MB shared cache

    val options = ColumnFamilyOptions()
      .setCompressionType(CompressionType.NO_COMPRESSION)
      .setCompactionStyle(CompactionStyle.LEVEL)
      .setWriteBufferSize(64 * 1024 * 1024) // Larger memtable for better read performance
      .setMaxWriteBufferNumber(3)
      .setMinWriteBufferNumberToMerge(2)
      .setLevel0FileNumCompactionTrigger(4)
      .setLevel0SlowdownWritesTrigger(8)
      .setLevel0StopWritesTrigger(12)
      .setTargetFileSizeBase(128 * 1024 * 1024)
      .setMaxBytesForLevelBase(512 * 1024 * 1024) // Larger base level
      .optimizeForPointLookup(256 * 1024 * 1024) // Optimized for graph lookups
      .setBloomLocality(1)
      .setOptimizeFiltersForHits(true)
      .setTableFormatConfig(
        BlockBasedTableConfig()
          .setBlockCache(sharedBlockCache) // Shared cache
          .setBlockSize(32 * 1024) // Larger blocks for better compression of sequential IDs
          .setCacheIndexAndFilterBlocks(true)
          .setPinL0FilterAndIndexBlocksInCache(false)
          .setPinTopLevelIndexAndFilter(false) // Keep top-level index in memory
          .setFilterPolicy(BloomFilter(10.0, false)) // Bloom filter with 10 bits per key
          .setWholeKeyFiltering(true) // Enable whole key bloom filtering
          .setFormatVersion(5) // Latest format with better bloom filter support
          .setIndexType(IndexType.kTwoLevelIndexSearch) // Two-level index for large datasets
          .setPartitionFilters(true) // Partition filters for better memory efficiency
          .setMetadataBlockSize(8 * 1024) // Larger metadata blocks
          .setCacheIndexAndFilterBlocksWithHighPriority(true) // Prioritize index/filter in cache
          .setOptimizeFiltersForMemory(true),
      )

    if (comparator != null) {
      val comparatorOptions = ComparatorOptions()
      comparatorOptions.setMaxReusedBufferSize(256) // so it can keep longer labels
      comparatorOptions.setUseDirectBuffer(true)
      comparatorOptions.setReusedSynchronisationType(ReusedSynchronisationType.THREAD_LOCAL)
      val comparator = object : AbstractComparator(comparatorOptions) {
        override fun name(): String = "${name}_comparator"

        override fun compare(a: ByteBuffer, b: ByteBuffer): Int = comparator.compare(a, b)
      }
      options.setComparator(comparator)
    }
    else {
      options.setComparator(BuiltinComparator.BYTEWISE_COMPARATOR)
    }

    val desc = ColumnFamilyDescriptor(
      name.toByteArray(charset = Charsets.UTF_8),
      options,
    )
    db.createColumnFamily(desc)
  }

  override fun <K : Any, V : Any> createKVStore(
    name: String,
    keyType: Class<K>,
    valueType: Class<V>,
    vararg hints: StorageHints,
  ): KVStoreBuilder<*, K, V> = when {
    DefaultStorageHints.USE_PAGED_STORE in hints -> RocksdbKVStoreBuilder(
      owner = this,
      name = name
    )

    else -> InMemoryKVStoreBuilder(owner = this, name = name, disposable = disposable)
  }

  override fun <T : Any> createFlatStore(
    name: String,
    type: Class<T>,
    vararg hints: StorageHints,
  ): FlatStoreBuilder<T> = InMemoryFlatStoreBuilder(owner = this, name = name)

  override fun save(force: Boolean) {
    for ((storage, handler) in flatStorages) {
      if (!force && storage is PersistentStoreWithModificationMarker) {
        if (!storage.wasModified) {
          return
        }
      }
      val buffer = UnsafeByteBufferCodecBuffer.allocateHeap()
      storage.write(UnsafeCodecContext, buffer)
      db.put(WRITE_OPTIONS, handler.key, buffer.buffer.array())
    }
  }

  override fun register(store: FlatPersistentStore) {
    synchronized(lock) {
      val handler = FlatStorageHandler(key = getInMemoryStorageKey(store))
      flatStorages.putIfAbsent(store, handler)

      val key = getInMemoryStorageKey(store)
      val value = db.get(READ_OPTIONS, key)
      if (value != null) {
        val buffer = UnsafeByteBufferCodecBuffer(ByteBuffer.wrap(value))
        store.read(UnsafeCodecContext, buffer)
      }
    }
  }

  override fun unregister(store: FlatPersistentStore) {
    synchronized(lock) {
      val handler = flatStorages.remove(store) ?: return
      val buffer = UnsafeByteBufferCodecBuffer.allocateHeap()
      store.write(UnsafeCodecContext, buffer)
      db.put(WRITE_OPTIONS, handler.key, buffer.buffer.array())
    }
  }

  override fun dispose() {
    save(force = true)
    flushQueue.close()
    flush()
    inMemoryColumn.close()
    for (handle in columnFamilyCache.values) {
      handle.close()
    }
    db.close()
  }

  private fun flush() {
    val opts = FlushOptions()
      .setWaitForFlush(true)
      .setAllowWriteStall(true)
    opts.use { opts -> db.flush(opts) }
  }

  private fun getInMemoryStorageKey(store: FlatPersistentStore): ByteArray =
    "IN_MEMORY_STORE.VALUE.${store.name}".toByteArray(charset = Charsets.UTF_8)
}
