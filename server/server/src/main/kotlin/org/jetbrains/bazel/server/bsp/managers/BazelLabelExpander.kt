package org.jetbrains.bazel.server.bsp.managers

import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bazel.workspacecontext.WorkspaceContext

class BazelLabelExpander(private val bazelRunner: BazelRunner) {
  suspend fun getAllPossibleTargets(targets: TargetsSpec, workspaceContext: WorkspaceContext): List<Label> {
    val command =
      bazelRunner.buildBazelCommand(workspaceContext) {
        query {
          addTargetsFromSpec(targets)
          options.addAll(listOf("--output=label", "--keep_going"))
        }
      }
    val result =
      bazelRunner
        .runBazelCommand(command, logProcessOutput = false, serverPidFuture = null, shouldLogInvocation = false)
        .waitAndGetResult(ensureAllOutputRead = true)

    // afaik bazel won't tell us if there was a FATAL error (i.e. the query had a wrong syntax and wasn't executed at all)
    // or if some targets failed but some succeeded
    if (result.stdoutLines.isEmpty() && result.isNotSuccess) {
      error(result.stderr)
    } else {
      // log the stderr here
      return result.stdoutLines
        .map { Label.parse(it) }
    }
  }
}
