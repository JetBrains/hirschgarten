package org.jetbrains.bazel.sync.workspace.mapper

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.label
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.server.BazelServerService
import org.jetbrains.bazel.sync.scope.FirstPhaseSync
import org.jetbrains.bazel.sync.scope.PartialProjectSync
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.mapper.normal.AspectBazelProjectMapper
import org.jetbrains.bazel.sync.workspace.mapper.phased.PhasedBazelProjectMapper
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetPhasedParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetSelector
import org.jetbrains.bsp.protocol.allSources
import java.nio.file.Path

@ApiStatus.Internal
object BazelWorkspaceResolver {
  suspend fun fetchWorkspace(
    project: Project,
    scope: ProjectSyncScope,
    allKnownTargets: List<Label>?,
    build: Boolean,
    taskId: TaskId,
  ): BazelResolvedWorkspace {
    return BazelServerService.getInstance(project).connection.runWithServer { server ->
      when (scope) {
        is FirstPhaseSync -> {
          val phasedSyncProject = server.workspaceBuildPhasedTargets(WorkspaceBuildTargetPhasedParams(taskId))
          val phasedMapper = PhasedBazelProjectMapper(
            bazelPathsResolver = server.bazelPathsResolver,
            workspaceContext = server.workspaceContext,
          )
          val targets = phasedMapper.mapTargets(phasedSyncProject.repoMapping, phasedSyncProject.modules)
          BazelResolvedWorkspace(
            workspaceName = null,
            repoMapping = phasedSyncProject.repoMapping,
            rootTargets = targets.map { it.id }.toSet(),
            targets = targets,
            fileToTarget = calculateFileToTarget(targets),
            hasError = phasedSyncProject.hasError,
          )
        }

        SecondPhaseSync, is PartialProjectSync -> {
          val selector =
            if (scope is PartialProjectSync) {
              WorkspaceBuildTargetSelector.SpecificTargets(scope.targetsToSync)
            }
            else {
              WorkspaceBuildTargetSelector.AllTargets
            }

          val syncProject = server.workspaceBuildTargets(WorkspaceBuildTargetParams(selector, build, allKnownTargets, taskId))

          syncProject.targets.values.filter { it.tagsList.any { it.equals(Constants.NO_IDE) } }.let {
            if (!it.isEmpty()) {
              project.syncConsole.addDiagnosticMessage(
                taskId, null, -1, -1,
                "Included ${it.size} ${Constants.NO_IDE} targets as dependencies: ${
                  it.joinToString(",", limit = 5) {
                    it.label().toString()
                  }
                }",
                MessageEvent.Kind.WARNING,
              )
            }
          }

          val bazelMapper =
            AspectBazelProjectMapper(
              project = project,
              server = server,
            )
          val targets = bazelMapper.mapTargets(
            allTargets = syncProject.targets,
            rootTargets = syncProject.rootTargets,
            repoMapping = syncProject.repoMapping,
          )

          BazelResolvedWorkspace(
            workspaceName = syncProject.workspaceName,
            repoMapping = syncProject.repoMapping,
            rootTargets = syncProject.rootTargets,
            targets = targets,
            fileToTarget = calculateFileToTarget(targets),
            hasError = syncProject.hasError,
          )
        }
      }
    }
  }

  private fun calculateFileToTarget(targets: List<RawBuildTarget>): Map<Path, List<RawBuildTarget>> {
    val resultMap = HashMap<Path, MutableList<RawBuildTarget>>()
    for (target in targets) {
      for (source in target.allSources) {
        resultMap.computeIfAbsent(source) { ArrayList() }.add(target)
      }
    }
    return resultMap
  }

}
