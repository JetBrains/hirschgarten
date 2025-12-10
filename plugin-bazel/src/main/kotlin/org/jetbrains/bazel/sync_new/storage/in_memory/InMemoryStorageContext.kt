package org.jetbrains.bazel.sync_new.storage.in_memory

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import com.intellij.openapi.util.Disposer
import org.jetbrains.bazel.sync_new.codec.readString
import org.jetbrains.bazel.sync_new.codec.writeString
import org.jetbrains.bazel.sync_new.storage.FlatPersistentStore
import org.jetbrains.bazel.sync_new.storage.FlatStoreBuilder
import org.jetbrains.bazel.sync_new.storage.KVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.LifecycleStoreContext
import org.jetbrains.bazel.sync_new.storage.PersistentStoreOwner
import org.jetbrains.bazel.sync_new.storage.SortedKVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.StorageContext
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.util.UnsafeByteBufferCodecBuffer
import org.jetbrains.bazel.sync_new.storage.util.UnsafeCodecContext
import java.io.RandomAccessFile
import java.nio.file.Files
import kotlin.io.path.exists

class InMemoryStorageContext(
  internal val project: Project,
  private val disposable: Disposable,
) : StorageContext, LifecycleStoreContext, PersistentStoreOwner, Disposable {
  private val stores: MutableMap<String, FlatPersistentStore> = HashMap()
  private val contents: MutableMap<String, UnsafeByteBufferCodecBuffer> = HashMap()
  private val lock = Any()
  private val file = project.getProjectDataPath("bazel-storage.dat")

  init {
    Disposer.register(disposable, this)

    synchronized(lock) {
      if (file.exists()) {
        try {
          RandomAccessFile(file.toFile(), "r").use { handle ->
            val buffer = UnsafeByteBufferCodecBuffer.allocateUnsafe(handle.length().toInt())
            handle.seek(0)
            handle.channel.read(buffer.buffer)
            buffer.buffer.flip()

            val size = buffer.readVarInt()
            for (n in 0 until size) {
              val storeName = buffer.readString()
              val size = buffer.readVarInt()
              val buffer = buffer.readBuffer(size)
              contents[storeName] = UnsafeByteBufferCodecBuffer(buffer)
            }
          }
        } catch (e: Throwable) {
          Files.deleteIfExists(file)
          logger<InMemoryStorageContext>().warn("Failed to load in-memory storage", e)
        }
      }
    }
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
    synchronized(lock) {
      for (store in stores) {
        val buffer = UnsafeByteBufferCodecBuffer.allocateUnsafe()
        store.value.write(UnsafeCodecContext, buffer)
        buffer.buffer.flip()
        contents[store.key] = buffer
      }

      RandomAccessFile(file.toFile(), "rw").use { file ->
        // TODO create memory mapped CodecBuffer
        // 64mb - small projects should fit there entirely
        val buffer = UnsafeByteBufferCodecBuffer.allocateUnsafe(64 * 1024 * 1024)
        buffer.writeVarInt(stores.size)
        for ((name, content) in contents) {
          buffer.writeString(name)

          buffer.writeVarInt(content.buffer.remaining())
          buffer.writeBuffer(content.buffer)
        }
        buffer.buffer.flip()
        file.channel.write(buffer.buffer)
      }
    }
  }

  override fun register(store: FlatPersistentStore) {
    synchronized(lock) {
      stores[store.name] = store
      val buffer = contents[store.name]
      if (buffer != null) {
        try {
          store.read(UnsafeCodecContext, buffer)
        } catch (e: Throwable) {
          logger<InMemoryStorageContext>().warn("Failed to read store ${store.name}", e)
        }
      }
    }
  }

  override fun unregister(store: FlatPersistentStore) {
    synchronized(lock) {
      stores[store.name] = store

      val buffer = UnsafeByteBufferCodecBuffer.allocateUnsafe()
      store.write(UnsafeCodecContext, buffer)
      contents[store.name] = buffer
    }
  }

  override fun dispose() {
    save(force = true)
  }
}
