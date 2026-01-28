package org.jetbrains.bazel.workspace.fileEvents

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Service(Service.Level.PROJECT)
internal class FileEventController {
  private val processingLock = Mutex()

  // synchronized collections do not guarantee thread-safety on clear(), hence manual synchronization
  private val eventQueue = mutableListOf<SimplifiedFileEvent>()

  /** @return `true` if these events were the first to be reported in the batch, `false` otherwise */
  fun addEvents(events: List<SimplifiedFileEvent>): Boolean {
    synchronized(this) {
      val eventIsFirstInQueue = eventQueue.isEmpty()
      eventQueue += events
      return eventIsFirstInQueue
    }
  }

  fun getEventsAndClear(): List<SimplifiedFileEvent> {
    synchronized(this) {
      val events = eventQueue.toList() // list copy
      eventQueue.clear()
      return events
    }
  }

  suspend fun processWithLock(action: suspend () -> Unit) {
    try {
      processingLock.withLock(this) {
        action()
      }
    } catch (_: IllegalStateException) {
      // Mutex::withLock was called with Mutex already locked - in that case, we just want not to start the processing
    }
  }

  fun isAnotherProcessingInProgress(): Boolean = processingLock.isLocked

  companion object {
    @JvmStatic
    fun getInstance(project: Project): FileEventController = project.service()
  }
}
