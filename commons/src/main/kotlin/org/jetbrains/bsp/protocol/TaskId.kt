package org.jetbrains.bsp.protocol

data class TaskGroupId(val id: String) {
  fun task(taskId: String): TaskId = TaskId(this, taskId)

  companion object {
    /**
     * Empty group, which explicitly does not reflect in UI
     */
    val EMPTY = TaskGroupId("")
  }
}

/**
 * Represents hierarchical tasks.
 * Note: the task tree presentation in UI does not match the TaskId hierarchy 1:1,
 * however, respects the parent-child relationship.
 */
data class TaskId(val taskGroupId: TaskGroupId, val id: String, val parent: TaskId? = null) {
  init {
    if (parent != null && parent.taskGroupId != taskGroupId)
      throw IllegalArgumentException("TaskId parent originId must be equal to originId")
  }

  override fun toString(): String {
    val sb = StringBuilder()

    var id: TaskId? = this
    while (id != null) {
      if (sb.isNotEmpty())
        sb.insert(0, '/')
      sb.insert(0, id.id)
      id = id.parent
    }

    return taskGroupId.id + ":" + sb.toString()
  }

  fun subTask(id: String) = TaskId(taskGroupId, id, this)
}
