package org.jetbrains.bsp.bazel.server.sync.firstPhase

import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target
import kotlinx.coroutines.runBlocking
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelInfo
import org.jetbrains.bsp.bazel.server.model.Label
import org.jetbrains.bsp.bazel.server.model.Project
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import java.nio.file.Path

class FirstPhaseProjectResolver(
  private val workspaceRoot: Path,
  private val bazelRunner: BazelRunner,
  private val workspaceContextProvider: WorkspaceContextProvider,
  private val bazelInfo: BazelInfo,
) {
  fun resolve(originId: String, cancelChecker: CancelChecker): Project =
    runBlocking {
      val workspaceContext = workspaceContextProvider.currentWorkspaceContext()
      val command =
        bazelRunner.buildBazelCommand {
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

      val project =
        Project(
          workspaceRoot = workspaceRoot.toUri(),
          modules = emptyList(),
          libraries = emptyMap(),
          goLibraries = emptyMap(),
          invalidTargets = emptyList(),
          nonModuleTargets = emptyList(),
          bazelRelease = bazelInfo.release,
          lightweightModules = targets.associateBy { Label.parse(it.rule.name) },
        )

      bazelProcess.waitAndGetResult(cancelChecker, true)

      project
    }
}
