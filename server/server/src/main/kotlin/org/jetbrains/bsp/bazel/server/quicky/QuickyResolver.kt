package org.jetbrains.bsp.bazel.server.quicky

import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target
import kotlinx.coroutines.runBlocking
import org.eclipse.lsp4j.jsonrpc.CancelChecker
import org.jetbrains.bsp.bazel.bazelrunner.BazelRunner
import org.jetbrains.bsp.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bsp.bazel.bazelrunner.utils.BazelRelease
import org.jetbrains.bsp.bazel.server.model.Label
import org.jetbrains.bsp.bazel.server.model.Project
import org.jetbrains.bsp.bazel.workspacecontext.WorkspaceContextProvider
import java.nio.file.Path

class QuickyResolver(
  private val workspaceRoot: Path,
  private val bazelRunner: BazelRunner,
  private val workspaceContextProvider: WorkspaceContextProvider,
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

      val a = bazelRunner.runBazelCommand(command, serverPidFuture = null, logProcessOutput = false, originId = originId)
      val c = a.process.inputStream

//      val reader = DelimitedMessageReader(c, Target.parser())

      val result = mutableListOf<Target>()
      var aa: Target? = null
      do {
//        System.err.println("LALALA")
        aa = Target.parseDelimitedFrom(c)
        System.err.println(aa)
        if (aa != null) {
          result.add(aa)
        }
      } while (aa != null)

      val b = a.waitAndGetResult(cancelChecker, true)

      val p1 =
        Project(
          workspaceRoot = workspaceRoot.toUri(),
          modules = emptyList(),
          libraries = emptyMap(),
          goLibraries = emptyMap(),
          invalidTargets = emptyList(),
          nonModuleTargets = emptyList(),
          bazelRelease = BazelRelease(1),
          lightweightModules = result.associateBy { Label.parse(it.rule.name) },
        )

      p1
//    val project = projectProvider.refreshAndGet(cancelChecker, build = build)
//    return bspMapper.workspaceTargets(project)
    }
}
