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
        languagePluginsService = project.service<LanguagePluginsService>(),
        featureFlags = featureFlags,
        bazelPathsResolver = paths.bazelPathsResolver,
        targetTagsResolver = TargetTagsResolver(),
        mavenCoordinatesResolver = MavenCoordinatesResolver(),
      )
    phasedMapper =
      PhasedBazelProjectMapper(bazelPathsResolver = paths.bazelPathsResolver, workspaceContext = workspaceContext)
    state = BazelWorkspaceSyncState.Initialized
  }

  private suspend fun syncWorkspace(build: Boolean, taskId: String): SyncedWorkspaceState {
    when (val state = state) {
      // if workspace was already sync or even resolved - return the available state and avoid recomputation
      is BazelWorkspaceSyncState.Resolved -> return state.synced
      is BazelWorkspaceSyncState.Synced -> return state.synced

      // if the workspace hasn't been initialized - initialize and proceed
      BazelWorkspaceSyncState.NotInitialized -> initWorkspace()

      // workspace is in a correct state to sync - proceed
      BazelWorkspaceSyncState.Unsynced, BazelWorkspaceSyncState.Initialized -> {
        // fall through
      }
    }
    val bazelProject = connection.runWithServer { server -> server.runSync(build, taskId) }
    val earlyProject = EarlyBazelSyncProject(bazelProject.targets, bazelProject.hasError)
    return SyncedWorkspaceState(earlyProject)
      .also { state = BazelWorkspaceSyncState.Synced(it) }
  }

  private suspend fun resolveWorkspace(scope: ProjectSyncScope, taskId: String): ResolvedWorkspaceState {
    val synced =
      when (val state = state) {
        // workspace is already resolved - return the available state and avoid recomputation
        is BazelWorkspaceSyncState.Resolved,
        -> return state.resolved

        // workspace is not in the correct state - try to pull the required state
        is BazelWorkspaceSyncState.NotInitialized,
        BazelWorkspaceSyncState.Initialized,
        BazelWorkspaceSyncState.Unsynced,
        -> syncWorkspace(false, taskId)

        // workspace is in available state - pass previous state data
        is BazelWorkspaceSyncState.Synced -> state.synced
      }

    val repoMapping = connection.runWithServer { server -> server.workspaceBazelRepoMapping() }
    val (project, workspace) =
      when (scope) {
        is FirstPhaseSync -> {
          val buildTargets =
            connection.runWithServer { server -> server.workspaceBuildPhasedTargets(WorkspaceBuildTargetPhasedParams(taskId)) }
          val context = PhasedBazelProjectMapperContext(repoMapping.repoMapping)
          val project =
            PhasedBazelMappedProject(
              targets = buildTargets.targets,
              hasError = synced.earlyProject.hasError,
            )
          project to phasedMapper.resolveWorkspace(context, project)
        }

        SecondPhaseSync, is PartialProjectSync -> {
          val selector =
            if (scope is PartialProjectSync) {
              WorkspaceBuildTargetSelector.SpecificTargets(scope.targetsToSync)
            } else {
              WorkspaceBuildTargetSelector.AllTargets
            }
          val buildTargets = connection.runWithServer { server -> server.workspaceBuildTargets(WorkspaceBuildTargetParams(selector)) }
          val workspaceContext = connection.runWithServer { server -> server.workspaceContext() }
          val workspace =
            bazelMapper.createProject(
              allTargets = synced.earlyProject.targets,
              rootTargets = buildTargets.rootTargets,
              workspaceContext = workspaceContext,
              featureFlags = featureFlags,
              repoMapping = repoMapping.repoMapping,
              hasError = synced.earlyProject.hasError,
            )
          project to workspace
        }
      }
    return ResolvedWorkspaceState(workspace)
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
    resolveWorkspace(scope, taskId).resolvedWorkspace

  override suspend fun getOrFetchSyncedProject(build: Boolean, taskId: String): EarlyBazelSyncProject =
    syncWorkspace(build, taskId).earlyProject
}
