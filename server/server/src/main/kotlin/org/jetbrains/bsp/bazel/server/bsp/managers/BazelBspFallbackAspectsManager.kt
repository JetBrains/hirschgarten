package org.jetbrains.bsp.bazel.server.bsp.managers

import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.server.model.Label
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider

class BazelBspFallbackAspectsManager(
  private val bazelRunner: BazelRunner,
  private val workspaceContextProvider: WorkspaceContextProvider,
) {
  fun getAllPossibleTargets(cancelChecker: CancelChecker): List<Label> {
    val targets = workspaceContextProvider.currentWorkspaceContext().targets
    val command =
      bazelRunner.buildBazelCommand {
        query {
          addTargetsFromSpec(targets)
          options.addAll(listOf("--output=label", "--keep_going"))
        }
      }
    return bazelRunner
      .runBazelCommand(command, logProcessOutput = false, serverPidFuture = null)
      .waitAndGetResult(cancelChecker, ensureAllOutputRead = true)
      .stdoutLines
      .map { Label.parse(it) }
  }
}
