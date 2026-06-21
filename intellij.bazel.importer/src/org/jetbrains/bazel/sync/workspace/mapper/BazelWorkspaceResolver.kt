package org.jetbrains.bazel.sync.workspace.mapper

import com.google.devtools.intellij.ideinfo.IntellijIdeInfo
import com.intellij.build.events.MessageEvent
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelImporterBundle
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.label
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.server.BazelServerService
import org.jetbrains.bazel.sync.scope.FirstPhaseSync
import org.jetbrains.bazel.sync.scope.PartialProjectSync
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.mapper.phased.PhasedBazelProjectMapper
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetPhasedParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetSelector
import java.io.IOException
import java.nio.file.Path
import kotlin.collections.joinToString
import kotlin.io.path.exists
import kotlin.io.path.readLines

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
            rootTargets = targets.map { it.key }.toSet(),
            targets = targets,
            hasError = phasedSyncProject.hasError,
            configurations = emptyMap(),
          )
        }

        SecondPhaseSync, is PartialProjectSync -> {
          reportIgnoredBazelBsp(project, taskId, server.bazelInfo.workspaceRoot)

          val selector =
            if (scope is PartialProjectSync) {
              WorkspaceBuildTargetSelector.SpecificTargets(scope.targetsToSync)
            }
            else {
              WorkspaceBuildTargetSelector.AllTargets
            }

          val syncProject = server.workspaceBuildTargets(WorkspaceBuildTargetParams(selector, build, allKnownTargets, taskId))
          reportImportedNoIdeTargets(project, taskId, syncProject.targets.values)

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
            hasError = syncProject.hasError,
            configurations = syncProject.configurations,
          )
        }
      }
    }
  }

  private fun reportImportedNoIdeTargets(
    project: Project,
    taskId: TaskId,
    targets: Collection<IntellijIdeInfo.TargetIdeInfo>
  ) {
    val noIdeTargets = targets.filter { target -> target.tagsList.any { it.equals(Constants.NO_IDE) } }
    if (noIdeTargets.isNotEmpty()) {
      project.syncConsole.addDiagnosticMessage(
        taskId, null, -1, -1,
        message = BazelImporterBundle.message("bazel.import.noide.targets", noIdeTargets.size, Constants.NO_IDE),
        description = noIdeTargets.joinToString(",", limit = 5) {
          it.label().toString()
        },
        MessageEvent.Kind.WARNING,
      )
    }
  }

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-3170/Support-bazel-ignore-files
  private fun reportIgnoredBazelBsp(project: Project, taskId: TaskId, workspaceRoot: Path) {
    val bazelIgnore = workspaceRoot.resolve(Constants.BAZEL_IGNORE_FILE_NAME)
    if (!bazelIgnore.exists()) return

    val lines = try {
      bazelIgnore.readLines()
    } catch (_: IOException) {
      emptyList()
    }
    if (lines.contains(Constants.DOT_BAZELBSP_DIR_NAME)) {
      project.syncConsole.addDiagnosticMessage(
        taskId, null, -1, -1,
        message = BazelImporterBundle.message("bazel.import.ignored.bazelbsp", bazelIgnore, Constants.DOT_BAZELBSP_DIR_NAME),
        description = null,
        MessageEvent.Kind.ERROR,
      )
    }
  }
}
