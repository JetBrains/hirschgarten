package org.jetbrains.bazel.server.bsp.managers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.server.bep.BepServer
import org.jetbrains.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bazel.commons.TargetCollection
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.JoinedBuildClient
import java.nio.file.Path

// TODO: remove this file once we untangle the spaghetti and use the method from ExecuteService

class BazelBspCompilationManager(
  private val bazelRunner: BazelRunner,
  private val bazelPathsResolver: BazelPathsResolver,
  val client: JoinedBuildClient,
  val workspaceRoot: Path,
) {
  suspend fun buildTargetsWithBep(
    targetsSpec: TargetCollection,
    extraFlags: List<String> = emptyList(),
    originId: String?,
    environment: List<Pair<String, String>> = emptyList(),
    shouldLogInvocation: Boolean,
    workspaceContext: WorkspaceContext,
  ): BepBuildResult {
    val diagnosticsService = DiagnosticsService(workspaceRoot)
    val bepServer = BepServer(client, diagnosticsService, originId, bazelPathsResolver)
    val bepReader = BepReader(bepServer)
    return try {
      coroutineScope {
        val readerFuture =
          async(Dispatchers.Default) {
            bepReader.start()
          }
        val command =
          bazelRunner.buildBazelCommand(workspaceContext) {
            build {
              options.addAll(extraFlags)
              targets.addAll(targetsSpec.values)
              excludedTargets.addAll(targetsSpec.excludedValues)
              this.environment.putAll(environment)
              useBes(bepReader.eventFile.toPath().toAbsolutePath())
            }
          }
        val result =
          bazelRunner
            .runBazelCommand(command, originId = originId, serverPidFuture = bepReader.serverPid, shouldLogInvocation = shouldLogInvocation)
            .waitAndGetResult(true)
        bepReader.finishBuild()
        readerFuture.await()
        BepBuildResult(result, bepServer.bepOutput)
      }
    } finally {
      bepReader.finishBuild()
    }
  }
}
