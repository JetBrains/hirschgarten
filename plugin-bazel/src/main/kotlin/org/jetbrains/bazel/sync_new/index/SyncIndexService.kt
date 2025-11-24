package org.jetbrains.bazel.sync_new.index

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.jetbrains.bazel.sync_new.storage.BazelStorageService
import org.jetbrains.bazel.sync_new.storage.StorageContext

@Service(Service.Level.PROJECT)
class SyncIndexService(
  private val project: Project
) : SyncIndexContext, Disposable {
  override val storageContext: StorageContext
    get() = project.service<BazelStorageService>().context

  private val disposable = Disposer.newDisposable()
  private val indexes: MutableMap<String, SyncIndex> = mutableMapOf()

  override fun register(index: SyncIndex) {
    if (index is Disposable) {
      Disposer.register(disposable, index)
    }
    synchronized(indexes) {
      indexes.putIfAbsent(index.name, index)
    }
  }

  override fun unregister(index: SyncIndex) {
    synchronized(indexes) {
      indexes.remove(index.name)
    }
  }

  override fun dispose() {
    Disposer.dispose(disposable)
  }

  fun invalidateAll() {
    synchronized(indexes) {
      indexes.values.forEach { it.invalidateAll() }
    }
  }
}

val Project.syncIndexService: SyncIndexService
  get() = service()
