package org.jetbrains.bsp.bazel.server.sync

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bsp.bazel.server.benchmark.openTelemetry
import org.jetbrains.bsp.bazel.server.model.Project

class ProjectProvider(private val projectResolver: ProjectResolver) {
  private var project: Project? = null

  @Synchronized
  fun refreshAndGet(cancelChecker: CancelChecker, build: Boolean): Project =
    loadFromBazel(cancelChecker, build = build, null).also { project = it }

  @Synchronized
  fun get(cancelChecker: CancelChecker): Project = project ?: loadFromBazel(cancelChecker, false, null).also { project = it }

  @Synchronized
  fun loadFromBazel(
    cancelChecker: CancelChecker,
    build: Boolean,
    targetsToSync: List<BuildTargetIdentifier>?,
  ): Project =
    projectResolver.resolve(cancelChecker, build = build, targetsToSync).also {
      openTelemetry.sdkTracerProvider.forceFlush()
      projectResolver.releaseMemory()
    }
}
