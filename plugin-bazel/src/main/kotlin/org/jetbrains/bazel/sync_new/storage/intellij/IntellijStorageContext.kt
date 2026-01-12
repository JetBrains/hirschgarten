package org.jetbrains.bazel.sync_new.storage.intellij

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import org.jetbrains.bazel.sync_new.storage.KVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.DefaultStorageHints
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.in_memory.InMemoryStorageContext
import org.jetbrains.bazel.sync_new.storage.util.FileUtils
import java.nio.file.Files
import java.nio.file.Path

internal class IntellijStorageContext(
  project: Project,
  disposable: Disposable,
) : InMemoryStorageContext(project, disposable) {

  private val storages = mutableListOf<IntellijPersistentMapKVStore<*, *>>()

  override fun dispose() {
    super.dispose()
    for (store in storages) {
      store.close()
    }
  }

  override fun <K : Any, V : Any> createKVStore(
    name: String,
    keyType: Class<K>,
    valueType: Class<V>,
    vararg hints: StorageHints,
  ): KVStoreBuilder<*, K, V> =
    when {
      DefaultStorageHints.USE_PAGED_STORE in hints -> {
        val file = project.getProjectDataPath("bazel_intellij_kv")
          .resolve("$name.db")

        Files.createDirectories(file.parent)
        IntellijPersistentMapKVStoreBuilder(
          context = this,
          file = file,
        )
      }

      else -> super.createKVStore(name, keyType, valueType, *hints)
    }


  internal fun register(store: IntellijPersistentMapKVStore<*, *>) {
    synchronized(storages) {
      storages.add(store)
    }
  }

  override fun getStoreFile(storeName: String): Path {
    return project.getProjectDataPath("bazel-intellij-storage")
      .resolve("bazel_intellij_flat")
      .resolve("${FileUtils.sanitize(storeName)}.dat")
  }
}
