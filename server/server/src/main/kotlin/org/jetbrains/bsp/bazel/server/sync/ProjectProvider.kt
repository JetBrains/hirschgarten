package org.jetbrains.bsp.bazel.server.sync

import kotlinx.coroutines.runBlocking
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bsp.bazel.server.benchmark.openTelemetry
import org.jetbrains.bsp.bazel.server.model.Project
import org.jetbrains.bsp.bazel.server.sync.firstPhase.FirstPhaseProjectResolver

class ProjectProvider(private val projectResolver: ProjectResolver, private val firstPhaseProjectResolver: FirstPhaseProjectResolver) {
  private var project: Project? = null

  @Synchronized
  fun refreshAndGet(cancelChecker: CancelChecker, build: Boolean): Project =
    loadFromBazel(cancelChecker, build = build, null).also { project = it }

  @Synchronized
  fun updateAndGet(cancelChecker: CancelChecker, targetsToSync: List<Label>): Project =
    loadFromBazel(cancelChecker, build = false, targetsToSync).also { project = project?.plus(it) }

  @Synchronized
  fun get(cancelChecker: CancelChecker): Project = project ?: loadFromBazel(cancelChecker, false, null).also { project = it }

  @Synchronized
  fun bazelQueryRefreshAndGet(cancelChecker: CancelChecker, originId: String): Project =
    firstPhaseProjectResolver.resolve(originId, cancelChecker).also { project = it }

  @Synchronized
  private fun loadFromBazel(
    cancelChecker: CancelChecker,
    build: Boolean,
    targetsToSync: List<Label>?,
  ): Project =
    runBlocking {
      projectResolver.resolve(cancelChecker, build = build, targetsToSync).also {
        openTelemetry.sdkTracerProvider.forceFlush()
        projectResolver.releaseMemory()
      }
    }
}
