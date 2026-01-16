package org.jetbrains.bazel.server.sync.firstPhase

import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.server.bzlmod.calculateRepoMapping
import org.jetbrains.bazel.server.model.PhasedSyncProject
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import java.nio.file.Path

class FirstPhaseProjectResolver(
  private val workspaceRoot: Path,
  private val bazelRunner: BazelRunner,
  private val workspaceContext: WorkspaceContext,
  private val bazelInfo: BazelInfo,
  private val bspClientLogger: BspClientLogger,
) {
  suspend fun resolve(originId: String): PhasedSyncProject =
    coroutineScope {
      // Use the already available workspaceContext
      val command =
        bazelRunner.buildBazelCommand(workspaceContext) {
          query {
            options.add("--output=streamed_proto")
            options.add(BazelFlag.keepGoing())

            addTargetsFromExcludableList(workspaceContext.targets)
          }
        }

      val bazelProcess = bazelRunner.runBazelCommand(command, logProcessOutput = false, originId = originId)
      val inputStream = bazelProcess.process.inputStream

      val targets = generateSequence { Target.parseDelimitedFrom(inputStream) }
      val modules = targets.associateBy { Label.parse(it.rule.name) }

      val repoMapping =
        calculateRepoMapping(workspaceContext, bazelRunner, bazelInfo, bspClientLogger)

      val project =
        PhasedSyncProject(
          workspaceRoot = workspaceRoot,
          bazelRelease = bazelInfo.release,
          modules = modules,
          repoMapping = repoMapping,
          workspaceContext = workspaceContext,
        )

      bazelProcess.waitAndGetResult(true)

      project
    }
}
