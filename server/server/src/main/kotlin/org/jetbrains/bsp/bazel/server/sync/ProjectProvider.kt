package org.jetbrains.bsp.bazel.server.sync

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import com.google.devtools.build.lib.query2.proto.proto2api.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bsp.bazel.server.benchmark.openTelemetry
import org.jetbrains.bsp.bazel.server.bep.BepServer
import org.jetbrains.bsp.bazel.server.bsp.managers.BepReader
import org.jetbrains.bsp.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bsp.bazel.server.model.Project
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bsp.protocol.JoinedBuildClient
import java.nio.file.Path

class ProjectProvider(private val projectResolver: ProjectResolver) {
  private var project: Project? = null

  @Synchronized
  fun refreshAndGet(cancelChecker: CancelChecker, build: Boolean): Project =
    loadFromBazel(cancelChecker, build = build, null).also { project = it }

  @Synchronized
  fun qqsync(cancelChecker: CancelChecker, bazelRunner: BazelRunner, workspaceRoot: Path, bazelPathsResolver: BazelPathsResolver, client: JoinedBuildClient): Project =
    runBlocking {
      val diagnosticsService = DiagnosticsService(workspaceRoot)
      val bepServer = BepServer(client, diagnosticsService, null, BuildTargetIdentifier("//..."), bazelPathsResolver)
      val bepReader = BepReader(bepServer)
      val readerFuture =
        async(Dispatchers.Default) {
          bepReader.start()
        }

      val command = bazelRunner.buildBazelCommand {
        query {
          options.add("--output=streamed_proto")
          options.add("--keep_going")
          targets.add(BuildTargetIdentifier("//..."))
        }
      }
      val a = bazelRunner.runBazelCommand(command, serverPidFuture = null)
      val c = a.process.inputStream

      val result = mutableListOf<Build.Target>()
      do {
        val target = Build.Target.parseDelimitedFrom(c)
        if (target != null) {
          result.add(target)
        }
      } while (target != null)

      val b = a.waitAndGetResult(cancelChecker, true)

//    val project = projectProvider.refreshAndGet(cancelChecker, build = build)
//    return bspMapper.workspaceTargets(project)

      bepReader.finishBuild()
      readerFuture.await()

      Project(
        workspaceRoot = bazelPathsResolver.workspaceRoot(),
        modules = emptyList(),
        sourceToTarget = emptyMap(),
        libraries = emptyMap(),
        goLibraries = emptyMap(),
        invalidTargets = emptyList(),
        nonModuleTargets = emptyList(),
        bazelRelease = BazelRelease(7),
        qqsync = result
      ).also { project = it }
  }

  @Synchronized
  fun updateAndGet(cancelChecker: CancelChecker, targetsToSync: List<BuildTargetIdentifier>): Project =
    loadFromBazel(cancelChecker, build = false, targetsToSync).also { project = project?.plus(it) }

  @Synchronized
  fun get(cancelChecker: CancelChecker): Project = project ?: loadFromBazel(cancelChecker, false, null).also { project = it }

  @Synchronized
  private fun loadFromBazel(
    cancelChecker: CancelChecker,
    build: Boolean,
    targetsToSync: List<BuildTargetIdentifier>?,
  ): Project =
    projectResolver.resolve(cancelChecker, build = build, targetsToSync).also {
      openTelemetry.sdkTracerProvider.forceFlush()
      projectResolver.releaseMemory()
    }
}
