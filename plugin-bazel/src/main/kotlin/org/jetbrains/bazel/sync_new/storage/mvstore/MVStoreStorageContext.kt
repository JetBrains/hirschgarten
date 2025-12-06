package org.jetbrains.bazel.sync_new.storage.mvstore

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.openapi.util.Disposer
import com.intellij.util.io.mvstore.createOrResetMvStore
import com.intellij.util.io.mvstore.openOrResetMap
import org.h2.mvstore.MVMap
import org.h2.mvstore.WriteBuffer
import org.h2.mvstore.type.ByteArrayDataType
import org.h2.mvstore.type.StringDataType
import org.jetbrains.bazel.sync_new.codec.CodecBuffer
import org.jetbrains.bazel.sync_new.codec.CodecContext
import org.jetbrains.bazel.sync_new.storage.FlatPersistentStore
import org.jetbrains.bazel.sync_new.storage.FlatStoreBuilder
import org.jetbrains.bazel.sync_new.storage.KVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.LifecycleStoreContext
import org.jetbrains.bazel.sync_new.storage.PersistentStoreOwner
import org.jetbrains.bazel.sync_new.storage.PersistentStoreWithModificationMarker
import org.jetbrains.bazel.sync_new.storage.SortedKVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.StorageContext
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.in_memory.InMemoryFlatStoreBuilder
import org.jetbrains.bazel.sync_new.storage.in_memory.InMemoryKVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.in_memory.InMemorySortedKVStoreBuilder
import java.nio.ByteBuffer

object MVStoreCodecContext : CodecContext

// TODO: add option to override store hints - we can have registry flag for forcing all stores to be in memory ones
class MVStoreStorageContext(
  internal val project: Project,
  private val disposable: Disposable,
) : StorageContext, LifecycleStoreContext, PersistentStoreOwner, Disposable {

  internal val store = createOrResetMvStore(
    file = project.getProjectDataPath("bazel-mvstore.db"),
    readOnly = false,
    logSupplier = { project.thisLogger() },
  )

  internal val ownedStores: MutableMap<FlatPersistentStore, EmbeddedFlatStoreRWHandler> = mutableMapOf()
  internal val ownedStoresMap: MVMap<String, ByteArray> = openOrResetMap("IN_MEMORY_STORE") {
    val map = MVMap.Builder<String, ByteArray>()
    map.setKeyType(StringDataType.INSTANCE)
    map.setValueType(ByteArrayDataType.INSTANCE)
    return@openOrResetMap map
  }

  init {
    Disposer.register(disposable, this)
  }

  override fun <K, V> createKVStore(
    name: String,
    keyType: Class<K>,
    valueType: Class<V>,
    vararg hints: StorageHints,
  ): KVStoreBuilder<*, K, V> =
    when {
      //StorageHints.USE_PAGED_STORE in hints -> InMemoryKVStoreBuilder(
      //  owner = this,
      //  name = getInMemoryStoreName(name),
      //)

      else -> InMemoryKVStoreBuilder(
        owner = this,
        name = getInMemoryStoreName(name),
      )
    }

  override fun <K, V> createSortedKVStore(
    name: String,
    keyType: Class<K>,
    valueType: Class<V>,
    vararg hints: StorageHints,
  ): SortedKVStoreBuilder<*, K, V> =
    when {
      //StorageHints.USE_PAGED_STORE in hints -> MVStoreSortedKVStoreBuilder(
      //  storageContext = this,
      //  name = getPageStoreName(name),
      //  keyType = keyType,
      //  valueType = valueType,
      //)

      else -> InMemorySortedKVStoreBuilder(
        owner = this,
        name = getInMemoryStoreName(name),
      )
    }

  override fun <T> createFlatStore(
    name: String,
    type: Class<T>,
    vararg hints: StorageHints,
  ): FlatStoreBuilder<T> = InMemoryFlatStoreBuilder(
    owner = this,
    name = name,
  )

  override fun save(force: Boolean) {
    synchronized(ownedStores) {
      // write all modified flat stores
      for ((store, handler) in ownedStores) {
        val wasModified = store is PersistentStoreWithModificationMarker && store.wasModified
        if (force || wasModified) {
          handler.write { store.write(MVStoreCodecContext, this) }
        }
      }
    }
    // save main db store
    if (store.hasUnsavedChanges()) {
      store.tryCommit()
    }
  }

  internal fun <K, V> openOrResetMap(name: String, builder: () -> MVMap.Builder<K, V>) =
    openOrResetMap(store, name, builder()) { project.thisLogger() }

  internal fun getPageStoreName(name: String) = "PAGE_STORE.$name"
  internal fun getInMemoryStoreName(name: String) = "IN_MEMORY_STORE.$name"

  override fun register(store: FlatPersistentStore) {
    val handler = EmbeddedFlatStoreRWHandler(ownedStoresMap, store.name)

    // read as soon as possible
    handler.read { store.read(MVStoreCodecContext, this) }

    synchronized(ownedStores) {
      ownedStores[store] = handler
    }

    if (store is Disposable) {
      Disposer.register(disposable, store)
    }
  }

  override fun unregister(store: FlatPersistentStore) {
    val handler = synchronized(ownedStores) {
      ownedStores.remove(store) ?: error("the store was not registered")
    }

    // mandatory write on disposal, even if store was not modified
    handler.write { store.write(MVStoreCodecContext, this) }
  }

  override fun dispose() {
    save(force = true)
    store.close()
  }
}

internal class EmbeddedFlatStoreRWHandler(
  private val map: MVMap<String, ByteArray>,
  private val name: String,
) {
  fun write(op: CodecBuffer.() -> Unit) {
    val mvBuffer = WriteBuffer()
    val buffer = MVStoreWriteCodecBuffer(mvBuffer)
    try {
      op(buffer)
    } catch (e: Throwable) {
      e.printStackTrace()
    }
    map[name] = mvBuffer.buffer.array()
  }

  fun read(op: CodecBuffer.() -> Unit) {
    // do not invoke read when value is not present
    val value = map[name] ?: return
    val buffer = MVStoreReadCodecBuffer(ByteBuffer.wrap(value))
    op(buffer)
  }
}
