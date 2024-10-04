package org.jetbrains.bsp.bazel.server.sync

import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bsp.bazel.server.benchmark.openTelemetry
import org.jetbrains.bsp.bazel.server.model.GoLibrary
import org.jetbrains.bsp.bazel.server.model.Label
import org.jetbrains.bsp.bazel.server.model.Library
import org.jetbrains.bsp.bazel.server.model.Module
import org.jetbrains.bsp.bazel.server.model.NonModuleTarget
import org.jetbrains.bsp.bazel.server.model.Project
import java.net.URI

class ProjectProvider(private val projectResolver: ProjectResolver) {
  private var project: Project? = null

  @Synchronized
  fun refreshAndGet(cancelChecker: CancelChecker, build: Boolean): Project =
    loadFromBazel(cancelChecker, build = build, null).also { project = it }

  @Synchronized
  fun updateAndGet(cancelChecker: CancelChecker, targetsToSync: List<BuildTargetIdentifier>): Project =
    loadFromBazel(cancelChecker, build = true, targetsToSync).also {
      project = project?.copy(
        modules = (project?.modules.orEmpty().toSet() + it.modules.toSet()).toList(),
        sourceToTarget = project?.sourceToTarget.orEmpty() + it.sourceToTarget,
        libraries = (project?.libraries.orEmpty() + it.libraries)
      )
    }

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
