package org.jetbrains.bsp.bazel.server.bsp.managers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.io.IoBuilder
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.server.bep.BepServer
import org.jetbrains.bsp.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bsp.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bsp.protocol.JoinedBuildClient
import java.nio.file.Path

// TODO: remove this file once we untangle the spaghetti and use the method from ExecuteService

class BazelBspCompilationManager(
  private val bazelRunner: BazelRunner,
  private val bazelPathsResolver: BazelPathsResolver,
  val client: JoinedBuildClient,
  val workspaceRoot: Path,
) {

  val LOGGER = LogManager.getLogger(BazelBspCompilationManager::class.java)

  suspend fun buildTargetsWithBep(
    cancelChecker: CancelChecker,
    targetsSpec: TargetsSpec,
    extraFlags: List<String> = emptyList(),
    originId: String? = null,
    environment: List<Pair<String, String>> = emptyList(),
    shouldLogInvocation: Boolean,
  ): BepBuildResult {
    val target = targetsSpec.values.firstOrNull()
    val diagnosticsService = DiagnosticsService(workspaceRoot)
    val bepServer = BepServer(client, diagnosticsService, originId, target, bazelPathsResolver)
    val bepReader = BepReader(bepServer)
    return try {
      coroutineScope {
        val readerFuture = bepReader.start(this)
        val command =
          bazelRunner.buildBazelCommand {
            build {
              options.addAll(extraFlags)
              addTargetsFromSpec(targetsSpec)
              this.environment.putAll(environment)
              useBes(bepReader.eventFile.toPath().toAbsolutePath())
            }
          }

        val result = withContext(Dispatchers.IO) {
          bazelRunner
            .runBazelCommand(command, originId = originId, serverPidFuture = bepReader.serverPid, shouldLogInvocation = shouldLogInvocation)
            .waitAndGetResult(cancelChecker, true)
        }
        bepReader.finishBuild()
        readerFuture.await()

        BepBuildResult(result, bepServer.bepOutput)
      }
    } finally {
      bepReader.finishBuild()
    }
  }
}
