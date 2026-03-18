package org.jetbrains.bazel.workspace.fileEvents

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.util.concurrent.atomic.AtomicBoolean

@Service(Service.Level.PROJECT)
internal class FileEventQueueController {
  // synchronized collections do not guarantee thread-safety on clear(), hence manual synchronization
  private val eventQueue = ArrayList<List<SimplifiedFileEvent>>()
  private val processingBatch = AtomicBoolean(false)

  /** @return `true` if caller should start processing events, `false` otherwise */
  fun addEvents(events: List<SimplifiedFileEvent>): Boolean {
    synchronized(this) {
      val wasEmpty = eventQueue.isEmpty()
      eventQueue.add(events)
      return wasEmpty && !processingBatch.get()
    }
  }

  /** @return `true` if there was another batch to process, `false` otherwise */
  suspend fun withNextBatch(body: suspend (List<SimplifiedFileEvent>) -> Unit): Boolean {
    val batch =
      synchronized(this) {
        val batch = eventQueue.flatten().takeIf { it.isNotEmpty() }
        processingBatch.set(batch != null)
        eventQueue.clear()
        batch
      }

    if (batch == null)
      return false

    try {
      body(batch)
    } catch (ex: Throwable) {
      synchronized(this) {
        processingBatch.set(false)
        eventQueue.clear()
      }
      throw ex
    }
    return true
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): FileEventQueueController = project.service()
  }
}
