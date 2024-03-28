package org.jetbrains.plugins.bsp.services

import ch.epfl.scala.bsp4j.StatusCode
import com.intellij.build.events.MessageEvent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

internal typealias OriginId = String

internal typealias TaskId = String

public interface BspTaskListener {
  public fun onDiagnostic(
    textDocument: String,
    buildTarget: String,
    line: Int,
    character: Int,
    severity: MessageEvent.Kind,
    message: String,
  ) {
  }

  public fun onOutputStream(taskId: TaskId?, text: String) {}
  public fun onErrorStream(taskId: TaskId?, text: String) {}

  public fun onTaskStart(taskId: TaskId, parentId: TaskId?, message: String, data: Any?) {}
  public fun onTaskProgress(taskId: TaskId, message: String, data: Any?) {}
  public fun onTaskFinish(taskId: TaskId, message: String, status: StatusCode, data: Any?) {}

  public fun onLogMessage(message: String) {}
  public fun onShowMessage(message: String) {}
}

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

  fun existsListener(id: OriginId): Boolean =
    taskListeners.containsKey(id)

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
