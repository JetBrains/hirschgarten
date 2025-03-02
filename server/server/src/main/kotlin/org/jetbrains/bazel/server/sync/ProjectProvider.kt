package org.jetbrains.bazel.server.sync

import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.FirstPhaseProject
import org.jetbrains.bazel.server.model.Project
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseProjectResolver

class ProjectProvider(private val projectResolver: ProjectResolver, private val firstPhaseProjectResolver: FirstPhaseProjectResolver) {
  private var project: Project? = null

  @Synchronized
  fun refreshAndGet(build: Boolean): AspectSyncProject = loadFromBazel(build = build, null).also { project = it }

  @Synchronized
  fun updateAndGet(targetsToSync: List<Label>): AspectSyncProject =
    loadFromBazel(build = false, targetsToSync).also { project = (project as? AspectSyncProject)?.plus(it) }

  @Synchronized
  fun get(): Project = project ?: loadFromBazel(false, null).also { project = it }

  @Synchronized
  fun bazelQueryRefreshAndGet(originId: String): FirstPhaseProject = firstPhaseProjectResolver.resolve(originId).also { project = it }

  @Synchronized
  private fun loadFromBazel(build: Boolean, targetsToSync: List<Label>?): AspectSyncProject =
    runBlocking {
      projectResolver.resolve(build = build, targetsToSync, project as? FirstPhaseProject).also {
        projectResolver.releaseMemory()
      }
    }
}
