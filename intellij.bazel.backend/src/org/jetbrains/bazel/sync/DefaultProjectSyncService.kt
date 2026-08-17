package org.jetbrains.bazel.sync

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.sync.task.ProjectSyncTask
import org.jetbrains.bsp.protocol.TaskId

@Service(Service.Level.PROJECT)
internal class DefaultProjectSyncService(private val project: Project) : ProjectSyncService {
  @Volatile
  override var lastSyncTaskId: TaskId? = null
    private set

  override suspend fun sync(scope: ProjectSyncScope) {
    val syncTask = ProjectSyncTask(project, onSyncTaskStarted = { lastSyncTaskId = it })
    when (scope) {
      is ProjectSyncScope.Full ->
        if (scope.phased) {
          syncTask.phasedSync(runSecondPhase = BazelFeatureFlags.executeSecondPhaseOnSync, buildProject = scope.build)
        }
        else {
          syncTask.fullSync(buildProject = scope.build)
        }

      is ProjectSyncScope.Targets -> throw UnsupportedOperationException("not supported yet")

      is ProjectSyncScope.Files -> throw UnsupportedOperationException("not supported yet")
    }
  }
}
