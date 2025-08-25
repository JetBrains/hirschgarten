package org.jetbrains.bazel.server.sync

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.PhasedSyncProject
import org.jetbrains.bazel.server.model.Project
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseProjectResolver

class ProjectProvider(private val projectResolver: ProjectResolver, private val firstPhaseProjectResolver: FirstPhaseProjectResolver) {
  @Volatile
  private var project: Project? = null
  private val projectMutex = Mutex()

  suspend fun refreshAndGet(build: Boolean, originId: String): AspectSyncProject =
    projectMutex.withLock {
      loadFromBazel(build = build, null, originId).also { project = it }
    }

  suspend fun updateAndGet(targetsToSync: List<Label>): AspectSyncProject =
    projectMutex.withLock {
      loadFromBazel(build = false, targetsToSync).also { project = (project as? AspectSyncProject)?.plus(it) }
    }

  suspend fun get(): Project =
    projectMutex.withLock {
      project ?: loadFromBazel(false, null).also { project = it }
    }

  suspend fun bazelQueryRefreshAndGet(originId: String): PhasedSyncProject =
    projectMutex.withLock {
      firstPhaseProjectResolver.resolve(originId).also { project = it }
    }

  // No mutex needed because project is volatile
  fun getIfLoaded(): Project? = project

  private suspend fun loadFromBazel(
    build: Boolean,
    targetsToSync: List<Label>?,
    originId: String? = null,
  ): AspectSyncProject =
    projectResolver.resolve(build = build, targetsToSync, project as? PhasedSyncProject, originId).also {
      projectResolver.releaseMemory()
    }
}
