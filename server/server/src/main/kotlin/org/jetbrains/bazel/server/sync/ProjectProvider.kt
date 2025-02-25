package org.jetbrains.bazel.server.sync

import kotlinx.coroutines.runBlocking
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.server.benchmark.openTelemetry
import org.jetbrains.bazel.server.model.AspectSyncProject
import org.jetbrains.bazel.server.model.FirstPhaseProject
import org.jetbrains.bazel.server.model.Project
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseProjectResolver

class ProjectProvider(private val projectResolver: ProjectResolver, private val firstPhaseProjectResolver: FirstPhaseProjectResolver) {
  private var project: Project? = null

  @Synchronized
  fun refreshAndGet(cancelChecker: CancelChecker, build: Boolean): AspectSyncProject =
    loadFromBazel(cancelChecker, build = build, null).also { project = it }

  @Synchronized
  fun updateAndGet(cancelChecker: CancelChecker, targetsToSync: List<Label>): AspectSyncProject =
    loadFromBazel(cancelChecker, build = false, targetsToSync).also { project = (project as? AspectSyncProject)?.plus(it) }

  @Synchronized
  fun get(cancelChecker: CancelChecker): Project = project ?: loadFromBazel(cancelChecker, false, null).also { project = it }

  @Synchronized
  fun bazelQueryRefreshAndGet(cancelChecker: CancelChecker, originId: String): FirstPhaseProject =
    firstPhaseProjectResolver.resolve(originId, cancelChecker).also { project = it }

  @Synchronized
  private fun loadFromBazel(
    cancelChecker: CancelChecker,
    build: Boolean,
    targetsToSync: List<Label>?,
  ): AspectSyncProject =
    runBlocking {
      projectResolver.resolve(cancelChecker, build = build, targetsToSync, project as? FirstPhaseProject).also {
        openTelemetry.sdkTracerProvider.forceFlush()
        projectResolver.releaseMemory()
      }
    }
}
