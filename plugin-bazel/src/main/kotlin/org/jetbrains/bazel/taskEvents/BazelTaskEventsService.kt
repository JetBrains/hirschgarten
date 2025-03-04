package org.jetbrains.bazel.taskEvents

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

typealias OriginId = String

@Service(Service.Level.PROJECT)
class BazelTaskEventsService {
  private val log = logger<BazelTaskEventsService>()

  private val taskListeners: ConcurrentHashMap<OriginId, BazelTaskListener> = ConcurrentHashMap()

  private fun get(id: OriginId): BazelTaskListener? {
    val listener = taskListeners[id]
    if (listener == null) {
      log.warn("No task listener found for task $id")
    }
    return listener
  }

  fun existsListener(id: OriginId): Boolean = taskListeners.containsKey(id)

  fun saveListener(id: OriginId, listener: BazelTaskListener) {
    taskListeners[id] = listener
  }

  fun withListener(id: OriginId, block: BazelTaskListener.() -> Unit) {
    get(id)?.apply { block() }
  }

  fun removeListener(id: OriginId) {
    taskListeners.remove(id)
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project) = project.service<BazelTaskEventsService>()
  }
}
