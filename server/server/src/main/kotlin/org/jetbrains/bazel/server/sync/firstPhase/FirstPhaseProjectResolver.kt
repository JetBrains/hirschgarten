package org.jetbrains.bazel.server.sync.firstPhase

import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target
import kotlinx.coroutines.coroutineScope
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.server.bzlmod.calculateRepoMapping
import org.jetbrains.bazel.server.model.FirstPhaseProject
import org.jetbrains.bazel.workspacecontext.provider.WorkspaceContextProvider
import java.nio.file.Path

class FirstPhaseProjectResolver(
  private val workspaceRoot: Path,
  private val bazelRunner: BazelRunner,
  private val workspaceContextProvider: WorkspaceContextProvider,
  private val bazelInfo: BazelInfo,
  private val bspClientLogger: BspClientLogger,
) {
  suspend fun resolve(originId: String): FirstPhaseProject =
    coroutineScope {
      val workspaceContext = workspaceContextProvider.readWorkspaceContext()
      val command =
        bazelRunner.buildBazelCommand(workspaceContext) {
          query {
            options.add("--output=streamed_proto")
            options.add(BazelFlag.keepGoing())

            targets.addAll(workspaceContext.targets.values)
            excludedTargets.addAll(workspaceContext.targets.excludedValues)
          }
        }

      val bazelProcess = bazelRunner.runBazelCommand(command, serverPidFuture = null, logProcessOutput = false, originId = originId)
      val inputStream = bazelProcess.process.inputStream

      val targets = generateSequence { Target.parseDelimitedFrom(inputStream) }
      val modules = targets.associateBy { Label.parse(it.rule.name) }

      val repoMapping =
        calculateRepoMapping(workspaceContext, bazelRunner, bazelInfo, bspClientLogger)

      val project =
        FirstPhaseProject(
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
