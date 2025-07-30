package org.jetbrains.bazel.sync.workspace

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.BazelPathsResolver
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
class BazelWorkspaceResolveService(private val project: Project) {
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
    val languagePluginsService = createLanguagePluginsService(paths.bazelPathsResolver)
    bazelMapper =
      AspectBazelProjectMapper(
        languagePluginsService = languagePluginsService,
        bazelPathsResolver = paths.bazelPathsResolver,
        targetTagsResolver = TargetTagsResolver(),
        mavenCoordinatesResolver = MavenCoordinatesResolver(),
      )
    clientMapper =
      AspectClientProjectMapper(
        languagePluginsService = languagePluginsService,
        featureFlags = featureFlags,
        bazelPathsResolver = paths.bazelPathsResolver,
      )
    phasedMapper =
      PhasedBazelProjectMapper(
        bazelPathsResolver = paths.bazelPathsResolver,
      )
    state = BazelWorkspaceSyncState.Initialized
  }

  private suspend fun syncWorkspace(build: Boolean, taskId: String, force: Boolean) {
    when (this.state) {
      is BazelWorkspaceSyncState.Resolved,
      is BazelWorkspaceSyncState.Synced,
        -> if (!force) return

      BazelWorkspaceSyncState.NotInitialized -> initWorkspace()

      BazelWorkspaceSyncState.Unsynced, BazelWorkspaceSyncState.Initialized -> {
        /* fall through */
      }
    }
    val bazelProject = connection.runWithServer { server -> server.runSync(build, taskId) }
    val earlyProject = EarlyBazelSyncProject(bazelProject.targets, bazelProject.hasError)
    state = BazelWorkspaceSyncState.Synced(earlyProject)
  }

  private suspend fun resolveWorkspace(scope: ProjectSyncScope, taskId: String, force: Boolean) {
    when (state) {
      is BazelWorkspaceSyncState.Resolved,
        -> if (!force) return

      is BazelWorkspaceSyncState.NotInitialized,
      BazelWorkspaceSyncState.Initialized,
      BazelWorkspaceSyncState.Unsynced,
        -> syncWorkspace(false, taskId, force)

      is BazelWorkspaceSyncState.Synced -> {
        /* fall through */
      }
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
              hasError = state.earlyProject.hasError,
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
              targets = state.earlyProject.targets,
              rootTargets = buildTargets.rootTargets,
              workspaceContext = workspaceContext,
              featureFlags = featureFlags,
              repoMapping = repoMapping.repoMapping,
              hasError = state.earlyProject.hasError,
            )
          project to clientMapper.resolveWorkspace(project)
        }
      }
    state = BazelWorkspaceSyncState.Resolved(
      earlyProject = state.earlyProject,
      mappedProject = project,
      resolvedWorkspace = workspace,
    )
  }

  // TODO: check if locking is necessary
  // TODO: also try to invent better way of lazy resolving workspace
  suspend fun getOrFetchResolvedWorkspace(
    scope: ProjectSyncScope = SecondPhaseSync,
    taskId: String = PROJECT_SYNC_TASK_ID,
    force: Boolean = false,
  ): BazelResolvedWorkspace {
    return resolveWorkspace(scope, taskId, force).let { state.resolvedWorkspace }
  }

  suspend fun getOrFetchMappedProject(
    scope: ProjectSyncScope = SecondPhaseSync,
    taskId: String = PROJECT_SYNC_TASK_ID,
    force: Boolean = false,
  ): BazelMappedProject {
    return resolveWorkspace(scope, taskId, force).let { state.mappedProject }
  }

  suspend fun getOrFetchSyncedProject(
    build: Boolean = false,
    taskId: String = PROJECT_SYNC_TASK_ID,
    force: Boolean = false,
  ): EarlyBazelSyncProject {
    return syncWorkspace(build, taskId, force).let { state.earlyProject }
  }

  suspend fun <T> withEndpointProxy(func: suspend (BazelEndpointProxy) -> T): T {
    val project = getOrFetchMappedProject(
      scope = SecondPhaseSync,
      taskId = PROJECT_SYNC_TASK_ID,
      force = false,
    )
    return connection.runWithServer {
      val endpoints = BazelEndpointProxy(clientMapper, project, it)
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
