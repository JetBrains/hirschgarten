package org.jetbrains.bazel.taskEvents

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.BuildTaskHandler
import org.jetbrains.bsp.protocol.TaskFinishParams
import org.jetbrains.bsp.protocol.TaskStartParams
import java.util.concurrent.ConcurrentHashMap

typealias OriginId = String

class TaskListenerAlreadyExistsException(message: String) : IllegalStateException(message)

@Service(Service.Level.PROJECT)
class BazelTaskEventsService : BuildTaskHandler {

  private val taskListeners: ConcurrentHashMap<OriginId, BazelTaskListener> = ConcurrentHashMap()

  private fun get(id: OriginId): BazelTaskListener {
    val listener = taskListeners[id]
    require(listener != null) { "No task listener found for task $id" }
    return listener
  }

  fun saveListener(id: OriginId, listener: BazelTaskListener) {
    if (taskListeners.putIfAbsent(id, listener) != null) {
      throw TaskListenerAlreadyExistsException("Listener for task $id exists already")
    }
  }

  fun withListener(id: OriginId, block: BazelTaskListener.() -> Unit) {
    get(id).apply { block() }
  }

  fun removeListener(id: OriginId) {
    taskListeners.remove(id)
  }

  override fun onBuildTaskStart(params: TaskStartParams) {
    val taskId = params.taskId.id
    val originId = params.originId
    val maybeParent = params.taskId.parents.firstOrNull()

    val message = params.message ?: return

    withListener(originId) {
      onTaskStart(taskId, maybeParent, message, params.data)
    }
  }

  override fun onBuildTaskFinish(params: TaskFinishParams) {
    val taskId = params.taskId.id
    val originId = params.originId
    val maybeParent = params.taskId.parents.firstOrNull()

    val status = params.status

    val message = params.message ?: return

    withListener(originId) {
      onTaskFinish(taskId, maybeParent, message, status, params.data)
    }
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project) = project.service<BazelTaskEventsService>()
  }
}
