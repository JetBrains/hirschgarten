package org.jetbrains.bazel.server.sync.firstPhase

import com.google.devtools.build.lib.query2.proto.proto2api.Build.Target
import kotlinx.coroutines.coroutineScope
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.params.BazelFlag
import org.jetbrains.bazel.commons.BazelInfo
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.targets
import org.jetbrains.bazel.server.bzlmod.calculateRepoMapping
import org.jetbrains.bazel.server.model.PhasedSyncProject
import org.jetbrains.bsp.protocol.BazelTaskEventsHandler
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.asLogger
import java.nio.file.Path

@ApiStatus.Internal
class FirstPhaseProjectResolver(
  private val workspaceRoot: Path,
  private val bazelRunner: BazelRunner,
  private val projectView: ProjectView,
  private val bazelInfo: BazelInfo,
  private val taskEventsHandler: BazelTaskEventsHandler,
) {
  suspend fun resolve(taskId: TaskId): PhasedSyncProject =
    coroutineScope {
      val command =
        bazelRunner.buildBazelCommand(projectView) {
          query {
            options.add("--output=streamed_proto")
            options.add(BazelFlag.keepGoing())

            addTargetsFromExcludableList(projectView.targets)
          }
        }

      val bazelResult =
        bazelRunner
          .runBazelCommand(command, logProcessOutput = false, taskId = taskId)
          .waitAndGetResult()

      val inputStream = bazelResult.stdout.inputStream()
      val targets: Sequence<Target> = generateSequence { Target.parseDelimitedFrom(inputStream) }
      val modules: Map<Label, Target> = targets.associateBy { Label.parse(it.rule.name) }

      val repoMapping = calculateRepoMapping(projectView, bazelRunner, bazelInfo, taskEventsHandler.asLogger(taskId), taskId)

      PhasedSyncProject(
        workspaceRoot = workspaceRoot,
        bazelRelease = bazelInfo.release,
        modules = modules,
        repoMapping = repoMapping,
        hasError = bazelResult.isNotSuccess
      )
    }
}
