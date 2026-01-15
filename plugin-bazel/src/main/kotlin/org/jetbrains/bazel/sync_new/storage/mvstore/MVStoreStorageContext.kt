package org.jetbrains.bazel.sync_new.storage.mvstore

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.openapi.util.Disposer
import com.intellij.util.io.mvstore.createOrResetMvStore
import com.intellij.util.io.mvstore.openOrResetMap
import org.h2.mvstore.MVMap
import org.jetbrains.bazel.sync_new.codec.CodecBuffer
import org.jetbrains.bazel.sync_new.codec.CodecContext
import org.jetbrains.bazel.sync_new.storage.CompactingStoreContext
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
import org.jetbrains.bazel.sync_new.storage.util.PagedFileChannelCodecBuffer
import org.jetbrains.bazel.sync_new.storage.util.FileUtils
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists
import kotlin.time.Duration.Companion.milliseconds

object MVStoreCodecContext : CodecContext

class MVStoreStorageContext(
  internal val project: Project,
  private val disposable: Disposable,
) : StorageContext, LifecycleStoreContext, CompactingStoreContext, PersistentStoreOwner, Disposable {

  internal val store = createOrResetMvStore(
    file = project.getProjectDataPath("bazel-mvstore.db"),
    readOnly = false,
    logSupplier = { project.thisLogger() },
  )

  internal val ownedStores: MutableMap<FlatPersistentStore, FileFlatStoreRWHandler> = mutableMapOf()
  private val storeChannels: MutableMap<String, StoreFileChannel> = mutableMapOf()

  private data class StoreFileChannel(
    val channel: FileChannel,
    val path: Path,
  )

  init {
    Disposer.register(disposable, this)
  }

  override fun <K : Any, V : Any> createKVStore(
    name: String,
    keyType: Class<K>,
    valueType: Class<V>,
    vararg hints: StorageHints,
  ): KVStoreBuilder<*, K, V> =
    when {
      DefaultStorageHints.USE_PAGED_STORE in hints -> MVStoreSortedKVStoreBuilder(
        storageContext = this,
        name = getPageStoreName(name),
        keyType = keyType,
        valueType = valueType,
      )

      else -> InMemoryKVStoreBuilder(
        owner = this,
        name = getInMemoryStoreName(name),
        disposable = disposable,
      )
    }

  override fun <T : Any> createFlatStore(
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
    val storeFile = getStoreFileChannel(store.name)
    val handler = FileFlatStoreRWHandler(storeFile.channel)

    if (storeFile.path.exists() && Files.size(storeFile.path) > 0) {
      handler.read { store.read(MVStoreCodecContext, this) }
    }

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
    
    // Close all file channels
    storeChannels.values.forEach { storeFile ->
      try {
        storeFile.channel.close()
      }
      catch (e: Throwable) {
        project.thisLogger().warn("Failed to close channel for ${storeFile.path}", e)
      }
    }
    storeChannels.clear()
    
    store.close()
  }

  override suspend fun compact() {
    store.commit()
    store.compactFile(10.milliseconds.inWholeMilliseconds.toInt())
  }

  private fun getStoreFileChannel(storeName: String): StoreFileChannel {
    return storeChannels.getOrPut(storeName) {
      val path = getStoreFile(storeName)
      Files.createDirectories(path.parent)
      val channel = FileChannel.open(
        /* path = */ path,
        /* ...options = */ StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE
      )
      StoreFileChannel(channel, path)
    }
  }

  private fun getStoreFile(storeName: String): Path {
    return project.getProjectDataPath("bazel-flat-storages")
      .resolve("${FileUtils.sanitize(storeName)}.dat")
  }
}

internal class FileFlatStoreRWHandler(
  private val channel: FileChannel
) {
  fun write(op: CodecBuffer.() -> Unit) {
    try {
      channel.position(0)
      val buffer = PagedFileChannelCodecBuffer(channel)
      op(buffer)
      buffer.flush()
      channel.truncate(buffer.size.toLong())
    } catch (e: Throwable) {
      e.printStackTrace()
    }
  }

  fun read(op: CodecBuffer.() -> Unit) {
    channel.position(0)
    val buffer = PagedFileChannelCodecBuffer(channel)
    op(buffer)
  }
}
