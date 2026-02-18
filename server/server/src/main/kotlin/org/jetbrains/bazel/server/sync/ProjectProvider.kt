package org.jetbrains.bazel.server.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.PhasedSyncProject
import org.jetbrains.bazel.server.model.Project
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseProjectResolver
import org.jetbrains.bsp.protocol.TaskGroupId
import org.jetbrains.bsp.protocol.TaskId

class ProjectProvider(val projectResolver: ProjectResolver, private val firstPhaseProjectResolver: FirstPhaseProjectResolver) {
  @Volatile
  private var project: Project? = null
  private val projectMutex = Mutex()

  suspend fun refreshAndGet(build: Boolean, taskId: TaskId): AspectSyncProject =
    projectMutex.withLock {
      loadFromBazel(build = build, null, taskId).also { project = it }
    }

  suspend fun updateAndGet(targetsToSync: List<Label>, taskId: TaskId): AspectSyncProject =
    projectMutex.withLock {
      loadFromBazel(build = false, targetsToSync, taskId).also { project = (project as? AspectSyncProject)?.plus(it) }
    }

  suspend fun get(): Project =
    projectMutex.withLock {
      project ?: loadFromBazel(false, null, TaskGroupId.EMPTY.task("")).also { project = it }
    }

  suspend fun bazelQueryRefreshAndGet(taskId: TaskId): PhasedSyncProject =
    projectMutex.withLock {
      firstPhaseProjectResolver.resolve(taskId).also { project = it }
    }

  // No mutex needed because project is volatile
  fun getIfLoaded(): Project? = project

  private suspend fun loadFromBazel(
    build: Boolean,
    targetsToSync: List<Label>?,
    taskId: TaskId,
  ): AspectSyncProject =
    projectResolver.resolve(build = build, targetsToSync, project as? PhasedSyncProject, taskId).also {
      projectResolver.releaseMemory()
    }
}
