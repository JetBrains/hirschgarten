package org.jetbrains.bazel.sync

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import org.jetbrains.bsp.protocol.TaskId

@Service(Service.Level.PROJECT)
internal class DefaultProjectSyncService(private val project: Project) : ProjectSyncService {
  @Volatile
  override var lastSyncTaskId: TaskId? = null
    private set

  override suspend fun sync(scope: ProjectSyncScope) {
    val task = ProjectSyncTask(
      project = project,
      scope = scope,
      onSyncTaskStarted = { lastSyncTaskId = it },
    )
    task.sync()
  }
}
