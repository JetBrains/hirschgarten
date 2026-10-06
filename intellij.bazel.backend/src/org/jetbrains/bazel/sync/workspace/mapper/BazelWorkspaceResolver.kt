package org.jetbrains.bazel.sync.workspace.mapper

import com.intellij.build.events.MessageEvent
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelBackendBundle
import org.jetbrains.bazel.ignore.BazelIgnoreService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.progress.syncConsole
import org.jetbrains.bazel.progress.withSubtask
import org.jetbrains.bazel.server.BazelServerService
import org.jetbrains.bazel.server.bzlmod.extendRepoMapping
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetPhasedParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetSelector
import org.jetbrains.bsp.protocol.id
import java.nio.file.Path

@ApiStatus.Internal
object BazelWorkspaceResolver {
  suspend fun fetchPhasedWorkspace(project: Project, repoMapping: RepoMapping, taskId: TaskId): BazelResolvedWorkspace {
    return BazelServerService.getInstance(project).connection.runWithServer(taskId) { server ->
      val phasedSyncProject = server.workspaceBuildPhasedTargets(WorkspaceBuildTargetPhasedParams(taskId))
      val phasedMapper = PhasedBazelProjectMapper(
        bazelPathsResolver = server.bazelPathsResolver,
        projectView = server.projectView,
      )
      val targets = phasedMapper.mapTargets(phasedSyncProject.modules)
      BazelResolvedWorkspace(
        workspaceName = null,
        repoMapping = repoMapping,
        rootTargets = targets.map { it.key }.toSet(),
        targets = targets,
        hasError = phasedSyncProject.hasError,
        configurations = emptyMap(),
      )
    }
  }

  suspend fun fetchAspectWorkspace(
    project: Project,
    repoMapping: RepoMapping,
    allKnownTargets: List<Label>?,
    build: Boolean,
    taskId: TaskId,
    selector: WorkspaceBuildTargetSelector,
  ): BazelResolvedWorkspace {
    return BazelServerService.getInstance(project).connection.runWithServer(taskId) { server ->
      reportIgnoredBazelBsp(project, taskId, server.bazelInfo.workspaceRoot)

      val syncProject =
        server.workspaceBuildTargets(WorkspaceBuildTargetParams(selector, build, allKnownTargets, repoMapping, taskId))

      val targets = project.syncConsole.withSubtask(
        subtaskId = taskId.subTask("process_target_info"),
        message = BazelBackendBundle.message("progress.title.process.workspace.targets"),
      ) {
        AspectBazelProjectMapper(project = project, server = server)
          .mapTargets(targetInfoPaths = syncProject.targetProtoPaths, build = build, taskId = taskId)
      }

      reportImportedNoIdeTargets(project, taskId, targets)

      val extendedRepoMapping = extendRepoMapping(server, repoMapping, targets.map { it.id }, taskId)

      BazelResolvedWorkspace(
        workspaceName = targets.firstOrNull()?.workspaceName ?: "_main",
        repoMapping = extendedRepoMapping,
        rootTargets = syncProject.rootTargets,
        targets = targets,
        hasError = syncProject.hasError,
        configurations = syncProject.configurations,
      )
    }
  }

  private fun reportImportedNoIdeTargets(
    project: Project,
    taskId: TaskId,
    targets: Collection<BuildTarget>,
  ) {
    val noIdeTargets = targets.filter { target -> Constants.NO_IDE in target.tags }
    if (noIdeTargets.isNotEmpty()) {
      project.syncConsole.addDiagnosticMessage(
        taskId, null, -1, -1,
        message = BazelBackendBundle.message("bazel.import.noide.targets", noIdeTargets.size, Constants.NO_IDE),
        description = noIdeTargets.joinToString(",", limit = 5) {
          it.id.toString()
        },
        MessageEvent.Kind.WARNING,
      )
    }
  }

  private fun reportIgnoredBazelBsp(project: Project, taskId: TaskId, workspaceRoot: Path) {
    val dotBazelBsp = workspaceRoot.resolve(Constants.DOT_BAZELBSP_DIR_NAME)
    if (BazelIgnoreService.getInstance(project).isIgnored(dotBazelBsp)) {
      project.syncConsole.addDiagnosticMessage(
        taskId, null, -1, -1,
        message = BazelBackendBundle.message("bazel.import.ignored.bazelbsp", dotBazelBsp, Constants.DOT_BAZELBSP_DIR_NAME),
        description = null,
        MessageEvent.Kind.ERROR,
      )
    }
  }
}
