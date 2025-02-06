package org.jetbrains.plugins.bsp.impl.projectAware

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class BspSyncCache(project: Project) {
  private val map = mutableMapOf<String, Any>()

  init {
    project.messageBus.connect().subscribe(
      BspWorkspaceListener.TOPIC,
      object : BspWorkspaceListener {
        override fun syncStarted() {
        }

        override fun syncFinished(canceled: Boolean) {
          map.clear()
        }
      },
    )
  }

  fun getOrCompute(key: String, compute: () -> Any): Any =
    map.getOrPut(key, compute)

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BspSyncCache = project.service()
  }
}
