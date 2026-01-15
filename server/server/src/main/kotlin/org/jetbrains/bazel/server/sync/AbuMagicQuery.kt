package org.jetbrains.bazel.server.sync

import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.workspacecontext.WorkspaceContext

object AbuMagicQuery {
  suspend fun doMagic(bazelRunner: BazelRunner, workspaceContext: WorkspaceContext) {
    val clean = bazelRunner.buildBazelCommand(workspaceContext) { clean() }
    bazelRunner.runBazelCommand(clean, serverPidFuture = null, logProcessOutput = false).waitAndGetResult(true)
  }
}
