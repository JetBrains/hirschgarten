package org.jetbrains.bazel.server.connection

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.bazelrunner.BazelInfoResolver
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncherProvider
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.config.FeatureFlagsProvider
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.install.EnvironmentCreator
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionCheckerService
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.languages.projectview.ProjectViewToWorkspaceContextConverter
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.server.bsp.BspServerApi
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.bsp.managers.BazelBspAspectsManager
import org.jetbrains.bazel.server.bsp.managers.BazelBspLanguageExtensionsGenerator
import org.jetbrains.bazel.server.bsp.managers.BazelToolchainManager
import org.jetbrains.bazel.server.bsp.utils.InternalAspectsResolver
import org.jetbrains.bazel.server.client.BazelClient
import org.jetbrains.bazel.server.sync.BspProjectMapper
import org.jetbrains.bazel.server.sync.ExecuteService
import org.jetbrains.bazel.server.sync.ProjectProvider
import org.jetbrains.bazel.server.sync.ProjectResolver
import org.jetbrains.bazel.server.sync.ProjectSyncService
import org.jetbrains.bazel.server.sync.TargetInfoReader
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseProjectResolver
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseTargetToBspMapper
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.JoinedBuildServer
import java.util.concurrent.atomic.AtomicReference

class DefaultBazelServerConnection(private val project: Project) : BazelServerConnection {
  private val workspaceRoot = project.rootDir.toNioPath()
  private val environmentCreator = EnvironmentCreator(workspaceRoot).also {
    it.create()
  }
  private val server = AtomicReference<BspServerApi?>(null)

  override suspend fun <T> runWithServer(task: suspend (server: JoinedBuildServer) -> T): T {
    return task(getServer())
  }

  private suspend fun getServer(): BspServerApi {
    // ensure `.bazelbsp` directory exists and functions
    environmentCreator.create()

    val workspaceContext = ProjectViewToWorkspaceContextConverter.convert(
      projectView = ProjectViewService.getInstance(project).getProjectView(),
      workspaceRoot = project.rootDir.toNioPath(),
    )
    val bazelVersionUpdated = project.service<BazelVersionCheckerService>().updateCurrentVersion()
    var server = this.server.get()
    if (server == null ||
        bazelVersionUpdated ||
        server.workspaceContext != workspaceContext) {
      server = createServer(workspaceContext)
      this.server.set(server)
    }
    return server
  }

  private suspend fun createServer(workspaceContext: WorkspaceContext): BspServerApi {
    val client = BazelClient(project)
    val bspInfo = BspInfo(workspaceRoot)
    val bspClientLogger = BspClientLogger(client)
    val aspectsResolver = InternalAspectsResolver(bspInfo = bspInfo)
    val bazelInfoResolver = BazelInfoResolver(workspaceRoot)
    val bazelProcessLauncherProvider = BazelProcessLauncherProvider.getInstance()
    val bazelProcessLauncher =
      bazelProcessLauncherProvider.createBazelProcessLauncher(workspaceRoot, bspInfo, aspectsResolver, bazelInfoResolver)
    val bazelRunner = BazelRunner(bspClientLogger, workspaceRoot, bazelProcessLauncher)

    val bazelInfo = bazelInfoResolver.resolveBazelInfo(bazelRunner, workspaceContext)
    bazelInfo.release.deprecated()?.let { bspClientLogger.warn(it + " Sync might give incomplete results.") }
    val bazelPathsResolver = BazelPathsResolver(bazelInfo)

    val executeService =
      ExecuteService(
        project = project,
        workspaceRoot = workspaceRoot,
        client = client,
        bazelRunner = bazelRunner,
        workspaceContext = workspaceContext,
        bazelPathsResolver = bazelPathsResolver,
      )

    val bazelBspAspectsManager =
      BazelBspAspectsManager(
        executeService = executeService,
        aspectsResolver = aspectsResolver,
        bazelRelease = bazelInfo.release,
      )
    val bazelToolchainManager = BazelToolchainManager()
    val bazelBspLanguageExtensionsGenerator = BazelBspLanguageExtensionsGenerator(aspectsResolver)
    val targetInfoReader = TargetInfoReader(bspClientLogger)

    val projectResolver =
      ProjectResolver(
        bazelBspAspectsManager = bazelBspAspectsManager,
        bazelToolchainManager = bazelToolchainManager,
        bazelBspLanguageExtensionsGenerator = bazelBspLanguageExtensionsGenerator,
        workspaceContext = workspaceContext,
        featureFlags = FeatureFlagsProvider.getFeatureFlags(project),
        targetInfoReader = targetInfoReader,
        bazelInfo = bazelInfo,
        bazelRunner = bazelRunner,
        bazelPathsResolver = bazelPathsResolver,
        bspClientLogger = bspClientLogger,
      )
    val firstPhaseProjectResolver =
      FirstPhaseProjectResolver(
        workspaceRoot = workspaceRoot,
        bazelRunner = bazelRunner,
        workspaceContext = workspaceContext,
        bazelInfo = bazelInfo,
        bspClientLogger = bspClientLogger,
      )
    val projectProvider = ProjectProvider(projectResolver, firstPhaseProjectResolver)

    val bspProjectMapper =
      BspProjectMapper(
        bazelRunner = bazelRunner,
        bspInfo = bspInfo,
      )
    val firstPhaseTargetToBspMapper = FirstPhaseTargetToBspMapper()
    val projectSyncService =
      ProjectSyncService(bspProjectMapper, firstPhaseTargetToBspMapper, projectProvider, bazelInfo, workspaceContext)

    return BspServerApi(
      projectSyncService = projectSyncService,
      executeService = executeService,
      workspaceContext = workspaceContext,
      bazelPathsResolver = bazelPathsResolver,
      bazelInfo = bazelInfo,
    )
  }
}
