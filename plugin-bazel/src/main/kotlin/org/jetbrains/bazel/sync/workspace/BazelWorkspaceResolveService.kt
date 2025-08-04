package org.jetbrains.bazel.sync.workspace

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.EnvironmentProvider
import org.jetbrains.bazel.config.FeatureFlagsProvider
import org.jetbrains.bazel.server.connection.BazelServerConnection
import org.jetbrains.bazel.server.connection.BazelServerService
import org.jetbrains.bazel.sync.scope.FirstPhaseSync
import org.jetbrains.bazel.sync.scope.PartialProjectSync
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.workspace.languages.LanguagePluginsService
import org.jetbrains.bazel.sync.workspace.languages.go.GoLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.java.JavaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.java.JdkResolver
import org.jetbrains.bazel.sync.workspace.languages.java.JdkVersionResolver
import org.jetbrains.bazel.sync.workspace.languages.kotlin.KotlinLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.python.PythonLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.scala.ScalaLanguagePlugin
import org.jetbrains.bazel.sync.workspace.languages.thrift.ThriftLanguagePlugin
import org.jetbrains.bazel.sync.workspace.mapper.BazelMappedProject
import org.jetbrains.bazel.sync.workspace.mapper.EarlyBazelSyncProject
import org.jetbrains.bazel.sync.workspace.mapper.normal.AspectBazelProjectMapper
import org.jetbrains.bazel.sync.workspace.mapper.normal.AspectClientProjectMapper
import org.jetbrains.bazel.sync.workspace.mapper.normal.MavenCoordinatesResolver
import org.jetbrains.bazel.sync.workspace.mapper.normal.TargetTagsResolver
import org.jetbrains.bazel.sync.workspace.mapper.phased.PhasedBazelMappedProject
import org.jetbrains.bazel.sync.workspace.mapper.phased.PhasedBazelProjectMapper
import org.jetbrains.bazel.sync.workspace.mapper.phased.PhasedBazelProjectMapperContext
import org.jetbrains.bazel.ui.console.ids.PROJECT_SYNC_TASK_ID
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetPhasedParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetSelector

@Service(Service.Level.PROJECT)
class BazelWorkspaceResolveService(private val project: Project) : BazelWorkspaceResolver {
  val connection: BazelServerConnection
    get() = BazelServerService.getInstance(project).connection

  private val featureFlags = FeatureFlagsProvider.getFeatureFlags(project)

  lateinit var bazelMapper: AspectBazelProjectMapper
  lateinit var clientMapper: AspectClientProjectMapper
  lateinit var phasedMapper: PhasedBazelProjectMapper

  private var state: BazelWorkspaceSyncState = BazelWorkspaceSyncState.NotInitialized

  private suspend fun initWorkspace() {
    when (this.state) {
      BazelWorkspaceSyncState.Initialized,
      BazelWorkspaceSyncState.Unsynced,
      is BazelWorkspaceSyncState.Resolved,
      is BazelWorkspaceSyncState.Synced,
        -> return

      BazelWorkspaceSyncState.NotInitialized -> {
        /* fall through */
      }
    }
    val paths = connection.runWithServer { server -> server.workspaceBazelPaths() }
    val workspaceContext = connection.runWithServer { server -> server.workspaceContext() }
    val languagePluginsService = createLanguagePluginsService(paths.bazelPathsResolver)
    bazelMapper =
      AspectBazelProjectMapper(
        languagePluginsService = languagePluginsService,
        bazelPathsResolver = paths.bazelPathsResolver,
        targetTagsResolver = TargetTagsResolver(),
        environmentProvider = EnvironmentProvider.getInstance(),
        mavenCoordinatesResolver = MavenCoordinatesResolver(),
      )
    clientMapper =
      AspectClientProjectMapper(
        languagePluginsService = languagePluginsService,
        featureFlags = featureFlags,
        bazelPathsResolver = paths.bazelPathsResolver,
      )
    phasedMapper =
      PhasedBazelProjectMapper(bazelPathsResolver = paths.bazelPathsResolver, workspaceContext = workspaceContext)
    state = BazelWorkspaceSyncState.Initialized
  }

  private suspend fun syncWorkspace(build: Boolean, taskId: String): SyncedWorkspaceState {
    when (val state = state) {
      is BazelWorkspaceSyncState.Resolved -> return state.synced
      is BazelWorkspaceSyncState.Synced -> return state.synced

      BazelWorkspaceSyncState.NotInitialized -> initWorkspace()

      BazelWorkspaceSyncState.Unsynced, BazelWorkspaceSyncState.Initialized -> {
        /* fall through */
      }
    }
    val bazelProject = connection.runWithServer { server -> server.runSync(build, taskId) }
    val earlyProject = EarlyBazelSyncProject(bazelProject.targets, bazelProject.hasError)
    return SyncedWorkspaceState(earlyProject)
      .also { state = BazelWorkspaceSyncState.Synced(it) }
  }

  private suspend fun resolveWorkspace(scope: ProjectSyncScope, taskId: String): ResolvedWorkspaceState {
    val synced = when (val state = state) {
      is BazelWorkspaceSyncState.Resolved,
        -> return state.resolved

      is BazelWorkspaceSyncState.NotInitialized,
      BazelWorkspaceSyncState.Initialized,
      BazelWorkspaceSyncState.Unsynced,
        -> syncWorkspace(false, taskId)

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
          val project =
            bazelMapper.createProject(
              targets = synced.earlyProject.targets,
              rootTargets = buildTargets.rootTargets,
              workspaceContext = workspaceContext,
              featureFlags = featureFlags,
              repoMapping = repoMapping.repoMapping,
              hasError = synced.earlyProject.hasError,
            )
          project to clientMapper.resolveWorkspace(project)
        }
      }
    return ResolvedWorkspaceState(project, workspace)
      .also { state = BazelWorkspaceSyncState.Resolved(synced, it) }
  }

  override suspend fun invalidateCachedState() {
    when (state) {
      is BazelWorkspaceSyncState.Resolved -> state = BazelWorkspaceSyncState.Initialized
      is BazelWorkspaceSyncState.Synced -> state = BazelWorkspaceSyncState.Initialized
      else -> {}
    }
  }

  override suspend fun getOrFetchResolvedWorkspace(
    scope: ProjectSyncScope,
    taskId: String,
  ): BazelResolvedWorkspace {
    return resolveWorkspace(scope, taskId).resolvedWorkspace
  }

  override suspend fun getOrFetchMappedProject(
    scope: ProjectSyncScope,
    taskId: String,
  ): BazelMappedProject {
    return resolveWorkspace(scope, taskId).mappedProject
  }

  override suspend fun getOrFetchSyncedProject(
    build: Boolean,
    taskId: String,
  ): EarlyBazelSyncProject {
    return syncWorkspace(build, taskId).earlyProject
  }

  override suspend fun <T> withEndpointProxy(func: suspend (BazelEndpointProxy) -> T): T {
    val project = getOrFetchMappedProject(
      scope = SecondPhaseSync,
      taskId = PROJECT_SYNC_TASK_ID,
    )
    return connection.runWithServer {
      val endpoints = DefaultBazelEndpointProxy(clientMapper, project, it)
      func(endpoints)
    }
  }

  private fun createLanguagePluginsService(bazelPathsResolver: BazelPathsResolver): LanguagePluginsService {
    val jdkResolver = JdkResolver(bazelPathsResolver, JdkVersionResolver())
    val javaLanguagePlugin = JavaLanguagePlugin(bazelPathsResolver, jdkResolver)
    val scalaLanguagePlugin = ScalaLanguagePlugin(javaLanguagePlugin, bazelPathsResolver)
    val kotlinLanguagePlugin = KotlinLanguagePlugin(javaLanguagePlugin, bazelPathsResolver)
    val thriftLanguagePlugin = ThriftLanguagePlugin(bazelPathsResolver)
    val pythonLanguagePlugin = PythonLanguagePlugin(bazelPathsResolver)
    val goLanguagePlugin = GoLanguagePlugin(bazelPathsResolver)

    return LanguagePluginsService(
      scalaLanguagePlugin,
      javaLanguagePlugin,
      kotlinLanguagePlugin,
      thriftLanguagePlugin,
      pythonLanguagePlugin,
      goLanguagePlugin,
    )
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelWorkspaceResolveService = project.getService(BazelWorkspaceResolveService::class.java)
  }
}
