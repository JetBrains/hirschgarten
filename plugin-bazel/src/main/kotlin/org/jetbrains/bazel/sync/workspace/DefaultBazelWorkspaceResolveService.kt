package org.jetbrains.bazel.sync.workspace

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.FeatureFlagsProvider
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
import org.jetbrains.bazel.sync.workspace.mapper.normal.TargetTagsResolver
import org.jetbrains.bazel.sync.workspace.mapper.phased.PhasedBazelMappedProject
import org.jetbrains.bazel.sync.workspace.mapper.phased.PhasedBazelProjectMapper
import org.jetbrains.bazel.sync.workspace.mapper.phased.PhasedBazelProjectMapperContext
import org.jetbrains.bsp.protocol.BazelProject
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetPhasedParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetSelector

@Service(Service.Level.PROJECT)
class DefaultBazelWorkspaceResolveService(private val project: Project) : BazelWorkspaceResolveService {
  val connection: BazelServerConnection
    get() = BazelServerService.getInstance(project).connection

  private val featureFlags = FeatureFlagsProvider.getFeatureFlags(project)

  lateinit var bazelMapper: AspectBazelProjectMapper
  lateinit var phasedMapper: PhasedBazelProjectMapper

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
    val workspaceContext = connection.runWithServer { server -> server.workspaceContext() }
    project
      .service<LanguagePluginsService>()
      .registerDefaultPlugins(paths.bazelPathsResolver, DefaultJvmPackageResolver())
    bazelMapper =
      AspectBazelProjectMapper(
        project = project,
        languagePluginsService = project.service<LanguagePluginsService>(),
        bazelPathsResolver = paths.bazelPathsResolver,
        targetTagsResolver = TargetTagsResolver(),
        mavenCoordinatesResolver = MavenCoordinatesResolver(),
      )
    phasedMapper =
      PhasedBazelProjectMapper(bazelPathsResolver = paths.bazelPathsResolver, workspaceContext = workspaceContext)
    state = BazelWorkspaceSyncState.Initialized
  }

  private suspend fun syncWorkspace(build: Boolean, taskId: String): BazelProject {
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

  private suspend fun resolveWorkspace(scope: ProjectSyncScope, taskId: String): BazelResolvedWorkspace {
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

    val repoMapping = connection.runWithServer { server -> server.workspaceBazelRepoMapping() }
    val workspace: BazelResolvedWorkspace =
      when (scope) {
        is FirstPhaseSync -> {
          val buildTargets =
            connection.runWithServer { server -> server.workspaceBuildPhasedTargets(WorkspaceBuildTargetPhasedParams(taskId)) }
          val context = PhasedBazelProjectMapperContext(repoMapping.repoMapping)
          val project =
            PhasedBazelMappedProject(
              targets = buildTargets.targets,
              hasError = synced.hasError,
            )
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
          val buildTargets = connection.runWithServer { server -> server.workspaceBuildTargets(WorkspaceBuildTargetParams(selector)) }
          val workspaceContext = connection.runWithServer { server -> server.workspaceContext() }
          bazelMapper.createProject(
            allTargets = synced.targets,
            rootTargets = buildTargets.rootTargets,
            workspaceContext = workspaceContext,
            featureFlags = featureFlags,
            repoMapping = repoMapping.repoMapping,
            hasError = synced.hasError,
          )
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

  override suspend fun getOrFetchResolvedWorkspace(scope: ProjectSyncScope, taskId: String): BazelResolvedWorkspace =
    resolveWorkspace(scope, taskId)

  override suspend fun getOrFetchSyncedProject(build: Boolean, taskId: String): BazelProject =
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
    data class Synced(val bazelProject: BazelProject) : BazelWorkspaceSyncState

    // project is fully resolved
    data class Resolved(val bazelProject: BazelProject, val resolvedWorkspace: BazelResolvedWorkspace) : BazelWorkspaceSyncState
  }
}
