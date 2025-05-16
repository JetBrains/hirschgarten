package org.jetbrains.bazel.sync

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.status.SyncStatusListener

@Service(Service.Level.PROJECT)
class SyncCache(project: Project) {
  private val map = mutableMapOf<String, Any>()

  init {
    project.messageBus.connect().subscribe(
      SyncStatusListener.TOPIC,
      object : SyncStatusListener {
        override fun syncStarted() {
        }

        override fun syncFinished(canceled: Boolean) {
          map.clear()
        }
      },
    )
  }

  @Synchronized
  fun getOrCompute(key: String, compute: () -> Any): Any = map.getOrPut(key, compute)

  companion object {
    @JvmStatic
    fun getInstance(project: Project): SyncCache = project.service()
  }
}
