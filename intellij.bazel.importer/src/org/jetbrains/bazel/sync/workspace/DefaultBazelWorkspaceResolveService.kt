package org.jetbrains.bazel.sync.workspace

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.server.connection.BazelServerConnection
import org.jetbrains.bazel.server.connection.BazelServerService
import org.jetbrains.bazel.sync.scope.FirstPhaseSync
import org.jetbrains.bazel.sync.scope.PartialProjectSync
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.workspace.languages.DefaultJvmPackageResolver
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginsService
import org.jetbrains.bazel.sync.workspace.mapper.normal.AspectBazelProjectMapper
import org.jetbrains.bazel.sync.workspace.mapper.normal.MavenCoordinatesResolver
import org.jetbrains.bazel.sync.workspace.mapper.phased.PhasedBazelMappedProject
import org.jetbrains.bazel.sync.workspace.mapper.phased.PhasedBazelProjectMapper
import org.jetbrains.bazel.sync.workspace.mapper.phased.PhasedBazelProjectMapperContext
import org.jetbrains.bsp.protocol.TaskId
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetPhasedParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetSelector
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult

@Service(Service.Level.PROJECT)
internal class DefaultBazelWorkspaceResolveService(private val project: Project) : BazelWorkspaceResolveService {
  val connection: BazelServerConnection
    get() = BazelServerService.getInstance(project).connection

  private var state: BazelWorkspaceSyncState = BazelWorkspaceSyncState.NotInitialized

  private suspend fun initWorkspace() {
    when (this.state) {
      // workspace is already initialized - abort
      BazelWorkspaceSyncState.Initialized,
      BazelWorkspaceSyncState.Unsynced,
      is BazelWorkspaceSyncState.Resolved,
      is BazelWorkspaceSyncState.Synced,
      -> return

      BazelWorkspaceSyncState.NotInitialized -> {
        // fall through
      }
    }
    val paths = connection.runWithServer { server -> server.workspaceBazelPaths() }
    val workspaceContext = connection.runWithServer { server -> server.workspaceContext }
    project
      .service<LanguagePluginsService>()
      .registerDefaultPlugins(paths.bazelPathsResolver, DefaultJvmPackageResolver(), workspaceContext)
    state = BazelWorkspaceSyncState.Initialized
  }

  private suspend fun syncWorkspace(build: Boolean, taskId: TaskId): WorkspaceBuildTargetsResult {
    when (val state = state) {
      // if workspace was already sync or even resolved - return the available state and avoid recomputation
      is BazelWorkspaceSyncState.Resolved -> return state.bazelProject
      is BazelWorkspaceSyncState.Synced -> return state.bazelProject

      // if the workspace hasn't been initialized - initialize and proceed
      BazelWorkspaceSyncState.NotInitialized -> initWorkspace()

      // workspace is in a correct state to sync - proceed
      BazelWorkspaceSyncState.Unsynced, BazelWorkspaceSyncState.Initialized -> {
        // fall through
      }
    }
    val bazelProject = connection.runWithServer { server -> server.runSync(build, taskId) }
    return bazelProject
      .also { state = BazelWorkspaceSyncState.Synced(it) }
  }

  private suspend fun resolveWorkspace(scope: ProjectSyncScope, taskId: TaskId): BazelResolvedWorkspace {
    val synced =
      when (val state = state) {
        // workspace is already resolved - return the available state and avoid recomputation
        is BazelWorkspaceSyncState.Resolved,
        -> return state.resolvedWorkspace

        // workspace is not in the correct state - try to pull the required state
        is BazelWorkspaceSyncState.NotInitialized,
        BazelWorkspaceSyncState.Initialized,
        BazelWorkspaceSyncState.Unsynced,
        -> syncWorkspace(false, taskId)

        // workspace is in available state - pass previous state data
        is BazelWorkspaceSyncState.Synced -> state.bazelProject
      }

    val workspace: BazelResolvedWorkspace =
      connection.runWithServer { server ->
        val repoMapping = server.workspaceBazelRepoMapping(taskId)
        when (scope) {
          is FirstPhaseSync -> {
            val buildTargets = server.workspaceBuildPhasedTargets(WorkspaceBuildTargetPhasedParams(taskId))
            val context = PhasedBazelProjectMapperContext(repoMapping.repoMapping)
            val project =
              PhasedBazelMappedProject(
                targets = buildTargets.targets,
                hasError = synced.hasError,
              )
            val phasedMapper = PhasedBazelProjectMapper(
              bazelPathsResolver = server.workspaceBazelPaths().bazelPathsResolver,
              workspaceContext = server.workspaceContext)
            phasedMapper.resolveWorkspace(context, project)
          }

          SecondPhaseSync, is PartialProjectSync -> {
            val selector =
              if (scope is PartialProjectSync) {
                WorkspaceBuildTargetSelector.SpecificTargets(scope.targetsToSync)
              }
              else {
                WorkspaceBuildTargetSelector.AllTargets
              }
            val buildTargets = server.workspaceBuildTargets(WorkspaceBuildTargetParams(selector, taskId))
            val bazelMapper =
              AspectBazelProjectMapper(
                project = project,
                languagePluginsService = project.service<LanguagePluginsService>(),
                bazelPathsResolver = server.workspaceBazelPaths().bazelPathsResolver,
                mavenCoordinatesResolver = MavenCoordinatesResolver(),
                outFilesHardLink = server.outFileHardLinks
              )
            bazelMapper.createProject(
              allTargets = synced.targets,
              rootTargets = buildTargets.rootTargets,
              workspaceContext = server.workspaceContext,
              repoMapping = repoMapping.repoMapping,
              hasError = synced.hasError,
            )
          }
        }
      }
    return workspace
      .also { state = BazelWorkspaceSyncState.Resolved(synced, it) }
  }

  override suspend fun invalidateCachedState() {
    when (state) {
      is BazelWorkspaceSyncState.Resolved -> state = BazelWorkspaceSyncState.Initialized
      is BazelWorkspaceSyncState.Synced -> state = BazelWorkspaceSyncState.Initialized
      else -> {}
    }
  }

  override suspend fun getOrFetchResolvedWorkspace(scope: ProjectSyncScope, taskId: TaskId): BazelResolvedWorkspace =
    resolveWorkspace(scope, taskId)

  override suspend fun getOrFetchSyncedProject(build: Boolean, taskId: TaskId): WorkspaceBuildTargetsResult =
    syncWorkspace(build, taskId)

  /**
   * Internal representation of [BazelWorkspaceResolveService] state.
   * [BazelWorkspaceSyncState] is used to lazily evaluate currently
   * required state without recomputing already available data
   */
  private sealed interface BazelWorkspaceSyncState {
    sealed class Nothing(val reason: String) : BazelWorkspaceSyncState

    object NotInitialized : Nothing("project is not initialized")

    // for example when project is being opened
    // TODO: we can avoid this state by serializing project state and restoring it on project open
    object Unsynced : Nothing("project is not synced")

    object Initialized : Nothing("project is not synced")

    // project only has raw target data
    data class Synced(val bazelProject: WorkspaceBuildTargetsResult) : BazelWorkspaceSyncState

    // project is fully resolved
    data class Resolved(val bazelProject: WorkspaceBuildTargetsResult, val resolvedWorkspace: BazelResolvedWorkspace) : BazelWorkspaceSyncState
  }
}
