package org.jetbrains.bazel.sync_new.storage.intellij

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getProjectDataPath
import org.jetbrains.bazel.sync_new.storage.KVStoreBuilder
import org.jetbrains.bazel.sync_new.storage.StorageHints
import org.jetbrains.bazel.sync_new.storage.in_memory.InMemoryStorageContext
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
  ): KVStoreBuilder<*, K, V> {
    val file = project.getProjectDataPath("bazel_intellij_kv")
      .resolve("$name.db")
    Files.createDirectories(file.parent)
    return IntellijPersistentMapKVStoreBuilder(
      context = this,
      file = file,
    )
  }

  internal fun register(store: IntellijPersistentMapKVStore<*, *>) {
    synchronized(storages) {
      storages.add(store)
    }
  }

  override fun getSaveFile(): Path {
    return project.getProjectDataPath("bazel-intellij-storage.dat")
  }
}
