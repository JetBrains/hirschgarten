package org.jetbrains.bsp.bazel.server.bsp.managers

import ch.epfl.scala.bsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.server.bep.BepServer
import org.jetbrains.bsp.bazel.server.diagnostics.DiagnosticsService
import org.jetbrains.bsp.bazel.server.model.Label
import org.jetbrains.bsp.bazel.server.paths.BazelPathsResolver
import org.jetbrains.bsp.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bsp.protocol.JoinedBuildClient
import java.nio.file.Path

// TODO: remove this file once we untangle the spaghetti and use the method from ExecuteService

class BazelBspCompilationManager(
  private val bazelRunner: BazelRunner,
  private val bazelPathsResolver: BazelPathsResolver,
  private val hasAnyProblems: MutableMap<Label, Set<TextDocumentIdentifier>>,
  val client: JoinedBuildClient,
  val workspaceRoot: Path,
) {
  fun buildTargetsWithBep(
    cancelChecker: CancelChecker,
    targetSpecs: TargetsSpec,
    extraFlags: List<String> = emptyList(),
    originId: String? = null,
    environment: List<Pair<String, String>> = emptyList(),
  ): BepBuildResult {
    val target = targetSpecs.values.firstOrNull()
    val diagnosticsService = DiagnosticsService(workspaceRoot, hasAnyProblems)
    val bepServer = BepServer(client, diagnosticsService, originId, target, bazelPathsResolver)
    val bepReader = BepReader(bepServer)
    return try {
      bepReader.start()
      val command =
        bazelRunner.buildBazelCommand {
          build {
            options.addAll(extraFlags)
            addTargetsFromSpec(targetSpecs)
            this.environment.putAll(environment)
            useBes(bepReader.eventFile.toPath().toAbsolutePath())
          }
        }
      val result =
        bazelRunner
          .runBazelCommand(command, originId = originId)
          .waitAndGetResult(cancelChecker, true)
      bepReader.finishBuild()
      bepReader.await()
      BepBuildResult(result, bepServer.bepOutput)
    } finally {
      bepReader.finishBuild()
    }
  }
}
