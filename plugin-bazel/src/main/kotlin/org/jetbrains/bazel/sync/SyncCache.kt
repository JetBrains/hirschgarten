package org.jetbrains.bazel.sync

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.status.SyncStatusListener

/**
 * Cache for storing arbitrary project data after sync.
 *
 * TODO: This is just a simple version inspired from OG, it needs redesigning.
 *
 * [BAZEL-2041](https://youtrack.jetbrains.com/issue/BAZEL-2041) Redesign project SyncCache
 */
@Service(Service.Level.PROJECT)
class SyncCache(project: Project) : Disposable {
  private var map = mutableMapOf<String, Any>()

  init {
    project.messageBus.connect(this).subscribe(
      SyncStatusListener.TOPIC,
      object : SyncStatusListener {
        override fun syncStarted() {
        }

        override fun syncFinished(canceled: Boolean) {
          clear()
        }
      },
    )
  }

  @Synchronized
  fun getOrCompute(key: String, compute: () -> Any): Any = map.getOrPut(key, compute)

  @Synchronized
  fun clear() {
    map = mutableMapOf()
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): SyncCache = project.service()
  }

  override fun dispose() {}
}
