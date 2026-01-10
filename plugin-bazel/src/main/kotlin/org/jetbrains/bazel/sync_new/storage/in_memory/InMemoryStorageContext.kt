package org.jetbrains.bazel.sync_new.storage.in_memory

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.bazel.sync_new.storage.FlatPersistentStore
import org.jetbrains.bazel.sync_new.storage.FlatStoreBuilder
import org.jetbrains.bazel.sync_new.storage.KVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.LifecycleStoreContext
import org.jetbrains.bazel.sync_new.storage.PersistentStoreOwner
import org.jetbrains.bazel.sync_new.storage.SortedKVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.StorageContext
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.util.FileChannelCodecBuffer
import org.jetbrains.bazel.sync_new.storage.util.FileUtils
import org.jetbrains.bazel.sync_new.storage.util.UnsafeByteBufferCodecBuffer
import org.jetbrains.bazel.sync_new.storage.util.UnsafeCodecContext
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.deleteIfExists
import kotlin.io.path.exists

open class InMemoryStorageContext(
  internal val project: Project,
  internal val disposable: Disposable,
) : StorageContext, LifecycleStoreContext, PersistentStoreOwner, Disposable {
  private data class StoreFileChannel(
    val channel: FileChannel,
    val path: Path,
  )

  private val stores: MutableMap<String, FlatPersistentStore> = HashMap()
  private val unregisteredStores: MutableMap<String, FlatPersistentStore> = HashMap()
  private val storeChannels: ConcurrentHashMap<String, StoreFileChannel> = ConcurrentHashMap()
  private val lock = Any()

  init {
    Disposer.register(disposable, this)
  }

  override fun <K : Any, V : Any> createKVStore(
    name: String,
    keyType: Class<K>,
    valueType: Class<V>,
    vararg hints: StorageHints,
  ): KVStoreBuilder<*, K, V> {
    return InMemoryKVStoreBuilder(this, name)
  }

  override fun <K : Any, V : Any> createSortedKVStore(
    name: String,
    keyType: Class<K>,
    valueType: Class<V>,
    vararg hints: StorageHints,
  ): SortedKVStoreBuilder<*, K, V> {
    return InMemorySortedKVStoreBuilder(this, name)
  }

  override fun <T : Any> createFlatStore(
    name: String,
    type: Class<T>,
    vararg hints: StorageHints,
  ): FlatStoreBuilder<T> {
    return InMemoryFlatStoreBuilder(this, name)
  }

  override fun save(force: Boolean) {
    if (!force) {
      return
    }
    runBlockingCancellable {
      val allStores = synchronized(lock) {
        stores + unregisteredStores
      }

      allStores.map { (storeName, store) ->
        async {
          try {
            val storeFile = getStoreFileChannel(storeName)
            storeFile.channel.position(0)
            val buffer = FileChannelCodecBuffer(storeFile.channel)
            store.write(UnsafeCodecContext, buffer)
            buffer.flush()
            storeFile.channel.truncate(buffer.size.toLong())
            storeFile.channel.force(true)
          }
          catch (e: Throwable) {
            logger<InMemoryStorageContext>().warn("Failed to save store $storeName", e)
            throw e
          }
        }
      }.awaitAll()

      synchronized(lock) {
        unregisteredStores.clear()
      }
    }
  }

  override fun register(store: FlatPersistentStore) {
    runBlockingCancellable {
      synchronized(lock) {
        stores[store.name] = store
      }
      val storeFile = getStoreFileChannel(store.name)

      if (storeFile.path.exists() && Files.size(storeFile.path) > 0) {
        try {
          storeFile.channel.position(0)
          val buffer = FileChannelCodecBuffer(storeFile.channel)
          store.read(
            ctx = UnsafeCodecContext,
            buffer = buffer,
          )
        }
        catch (e: Throwable) {
          logger<InMemoryStorageContext>().warn("Failed to read store ${store.name}", e)
        }
      }
    }
  }

  override fun unregister(store: FlatPersistentStore) {
    synchronized(lock) {
      stores.remove(store.name)
      unregisteredStores[store.name] = store
    }
  }

  override fun dispose() {
    save(force = true)
    storeChannels.values.forEach { storeFile ->
      try {
        storeFile.channel.close()
      }
      catch (e: Throwable) {
        logger<InMemoryStorageContext>().warn("Failed to close channel for ${storeFile.path}", e)
      }
    }
    storeChannels.clear()
    unregisteredStores.clear()
  }

  private fun getStoreFileChannel(storeName: String): StoreFileChannel {
    return storeChannels.computeIfAbsent(storeName) {
      val path = getStoreFile(storeName)
      Files.createDirectories(path.parent)
      val channel = FileChannel.open(
        /* path = */ path,
        /* ...options = */ StandardOpenOption.READ, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
      StoreFileChannel(channel, path)
    }
  }

  protected open fun getStoreFile(storeName: String): Path {
    return project.getProjectDataPath("bazel-storage")
      .resolve("${FileUtils.sanitize(storeName)}.dat")
  }
}

