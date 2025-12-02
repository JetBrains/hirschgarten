package org.jetbrains.bazel.sync_new.storage.rocksdb

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import org.jetbrains.bazel.sync_new.storage.FlatPersistentStore
import org.jetbrains.bazel.sync_new.storage.FlatStoreBuilder
import org.jetbrains.bazel.sync_new.storage.KVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.LifecycleStoreContext
import org.jetbrains.bazel.sync_new.storage.PersistentStoreOwner
import org.jetbrains.bazel.sync_new.storage.SortedKVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.StorageContext
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.rocksdb.AbstractComparator
import org.rocksdb.BuiltinComparator
import org.rocksdb.ColumnFamilyDescriptor
import org.rocksdb.ColumnFamilyHandle
import org.rocksdb.ColumnFamilyOptions
import org.rocksdb.CompactionStyle
import org.rocksdb.ComparatorOptions
import org.rocksdb.CompressionType
import org.rocksdb.Options
import org.rocksdb.ReusedSynchronisationType
import org.rocksdb.RocksDB
import java.nio.ByteBuffer
import kotlin.io.path.absolutePathString

class RocksdbStorageContext(
  internal val project: Project,
  private val disposable: Disposable,
) : StorageContext, LifecycleStoreContext, PersistentStoreOwner, Disposable {

  private val lock: Any = Any()
  private val columnFamilyCache: MutableMap<String, ColumnFamilyHandle> = mutableMapOf()
  private val db: RocksDB

  init {
    RocksDB.loadLibrary()

    val options = Options()
      .setCreateIfMissing(true)
      .setCreateMissingColumnFamilies(true)
      .optimizeUniversalStyleCompaction()
      .setAllowConcurrentMemtableWrite(true)
      .setAllowMmapReads(true)
      .setAllowMmapWrites(true)
      .setAvoidUnnecessaryBlockingIO(true)

    val path = project.getProjectDataPath("bazel-rocksdb.db")
    db = RocksDB.open(options, path.absolutePathString())
  }

  private fun createColumnFamily(name: String, comparator: Comparator<ByteBuffer>? = null): ColumnFamilyHandle = synchronized(lock) {
    if (name in columnFamilyCache) {
      error("Column family $name already exists")
    }

    val options = ColumnFamilyOptions()
      .setCompressionType(CompressionType.NO_COMPRESSION)
      .setCompactionStyle(CompactionStyle.UNIVERSAL)

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
    } else {
      options.setComparator(BuiltinComparator.BYTEWISE_COMPARATOR)
    }

    val desc = ColumnFamilyDescriptor(
      name.toByteArray(charset = Charsets.UTF_8),
      options
    )

    val handle = db.createColumnFamily(desc)
    columnFamilyCache[name] = handle
    return@synchronized handle
  }

  override fun <K, V> createKVStore(
    name: String,
    keyType: Class<K>,
    valueType: Class<V>,
    vararg hints: StorageHints,
  ): KVStoreBuilder<*, K, V> {
    TODO("Not yet implemented")
  }

  override fun <K, V> createSortedKVStore(
    name: String,
    keyType: Class<K>,
    valueType: Class<V>,
    vararg hints: StorageHints,
  ): SortedKVStoreBuilder<*, K, V> {
    TODO("Not yet implemented")
  }

  override fun <T> createFlatStore(
    name: String,
    type: Class<T>,
    vararg hints: StorageHints,
  ): FlatStoreBuilder<T> {
    TODO("Not yet implemented")
  }

  override fun save(force: Boolean) {
    TODO("Not yet implemented")
  }

  override fun register(store: FlatPersistentStore) {
    TODO("Not yet implemented")
  }

  override fun unregister(store: FlatPersistentStore) {
    TODO("Not yet implemented")
  }

  override fun dispose() {
    save(force = true)
    for ((_, handle) in columnFamilyCache) {
      handle.close()
    }
    db.close()
  }
}
