package org.jetbrains.plugins.bsp.services

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.run.BspTaskListener
import java.util.concurrent.ConcurrentHashMap

internal typealias OriginId = String

@Service(Service.Level.PROJECT)
internal class BspTaskEventsService {
  private val log = logger<BspTaskEventsService>()

  private val taskListeners: ConcurrentHashMap<OriginId, BspTaskListener> = ConcurrentHashMap()

  private fun get(id: OriginId): BspTaskListener? {
    val listener = taskListeners[id]
    if (listener == null) {
      log.warn("No task listener found for task $id")
    }
    return listener
  }

  fun existsListener(id: OriginId): Boolean = taskListeners.containsKey(id)

  fun saveListener(id: OriginId, listener: BspTaskListener) {
    taskListeners[id] = listener
  }

  fun withListener(id: OriginId, block: BspTaskListener.() -> Unit) {
    get(id)?.apply { block() }
  }

  fun removeListener(id: OriginId) {
    taskListeners.remove(id)
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project) = project.service<BspTaskEventsService>()
  }
}
