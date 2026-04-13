package org.jetbrains.bazel.server.connection

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.bazelrunner.BazelInfoResolver
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncherProvider
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.install.EnvironmentCreator
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionWorkspaceResolver
import org.jetbrains.bazel.server.bsp.BaselServerFacadeImpl
import org.jetbrains.bazel.server.bsp.managers.BazelBspAspectsManager
import org.jetbrains.bazel.server.bsp.managers.BazelBspLanguageExtensionsGenerator
import org.jetbrains.bazel.server.bsp.managers.BazelToolchainManager
import org.jetbrains.bazel.server.bsp.utils.InternalAspectsResolver
import org.jetbrains.bazel.server.sync.BazelSyncProjectProvider
import org.jetbrains.bazel.server.sync.BspProjectMapper
import org.jetbrains.bazel.server.sync.ExecuteService
import org.jetbrains.bazel.server.sync.ProjectResolver
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseProjectResolver
import org.jetbrains.bazel.sync.workspace.mapper.normal.DefaultBazelOutputFileHardLinks
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bazel.workspace.BazelExecutableProvider
import org.jetbrains.bazel.workspace.WorkspaceContextProvider
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.BazelServerFacade
import java.util.concurrent.atomic.AtomicReference

internal class DefaultBazelServerConnection(private val project: Project) : BazelServerConnection {
  private val workspaceRoot = project.rootDir.toNioPath()
  private val environmentCreator = EnvironmentCreator(workspaceRoot).also {
    it.create()
  }

  data class ServerWithVersionLiteral(val server: BaselServerFacadeImpl?, val versionLiteral: BazelVersionLiteral?)

  private val serverAndVersionLiteral = AtomicReference<ServerWithVersionLiteral>(ServerWithVersionLiteral(null, null))

  override suspend fun <T> runWithServer(task: suspend (server: BazelServerFacade) -> T): T {
    return task(getServer())
  }

  private suspend fun getServer(): BaselServerFacadeImpl {
    // ensure `.bazelbsp` directory exists and functions
    environmentCreator.create()

    val bazelExecutable = BazelExecutableProvider.computeBazelExecutableOrFail(project)
    val workspaceContext = project.serviceAsync<WorkspaceContextProvider>().computeWorkspaceContext(project, bazelExecutable)
    var (server, oldVersionLiteral) = this.serverAndVersionLiteral.get()
    val projectPath = project.rootDir.toNioPath()
    val resolvedVersion = BazelVersionWorkspaceResolver.resolveBazelVersionFromWorkspace(projectPath)
    val bazelVersionUpdated = oldVersionLiteral != resolvedVersion

    if (server == null ||
        bazelVersionUpdated ||
        server.workspaceContext != workspaceContext) {
      server = createServer(workspaceContext)
      this.serverAndVersionLiteral.set(ServerWithVersionLiteral(server, resolvedVersion))
    }
    return server
  }

  private suspend fun createServer(workspaceContext: WorkspaceContext): BaselServerFacadeImpl {
    val taskEventsHandler = BazelTaskEventsService.getInstance(project)
    val aspectsResolver = InternalAspectsResolver(workspaceRoot)
    val bazelInfoResolver = BazelInfoResolver(workspaceRoot)
    val bazelProcessLauncherProvider = BazelProcessLauncherProvider.getInstance()
    val bazelProcessLauncher =
      bazelProcessLauncherProvider.createBazelProcessLauncher(workspaceRoot, aspectsResolver, bazelInfoResolver)
    val bazelRunner = BazelRunner(taskEventsHandler, workspaceRoot, bazelProcessLauncher)

    val bazelInfo = bazelInfoResolver.resolveBazelInfo(bazelRunner, workspaceContext)
    val bazelPathsResolver = BazelPathsResolver(bazelInfo)

    val outFileHardLinks = DefaultBazelOutputFileHardLinks(project, bazelInfo)

    val executeService =
      ExecuteService(
        project = project,
        workspaceRoot = workspaceRoot,
        taskEventsHandler = taskEventsHandler,
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

    val projectResolver =
      ProjectResolver(
        bazelBspAspectsManager = bazelBspAspectsManager,
        bazelToolchainManager = bazelToolchainManager,
        bazelBspLanguageExtensionsGenerator = bazelBspLanguageExtensionsGenerator,
        workspaceContext = workspaceContext,
        bazelInfo = bazelInfo,
        bazelRunner = bazelRunner,
        bazelPathsResolver = bazelPathsResolver,
        taskEventsHandler = taskEventsHandler,
        project = project,
      )
    val firstPhaseProjectResolver =
      FirstPhaseProjectResolver(
        workspaceRoot = workspaceRoot,
        bazelRunner = bazelRunner,
        workspaceContext = workspaceContext,
        bazelInfo = bazelInfo,
        taskEventsHandler = taskEventsHandler,
      )
    val projectProvider = BazelSyncProjectProvider(projectResolver, firstPhaseProjectResolver)

    val bspProjectMapper =
      BspProjectMapper(
        workspaceRoot = workspaceRoot,
        bazelRunner = bazelRunner,
        workspaceContext = workspaceContext,
      )

    return BaselServerFacadeImpl(
      bspMapper = bspProjectMapper,
      projectProvider = projectProvider,
      executeService = executeService,
      workspaceContext = workspaceContext,
      bazelInfo = bazelInfo,
      outFileHardLinks = outFileHardLinks,
    )
  }
}

internal class BazelServerServiceImpl(project: Project) : BazelServerService {
  override val connection: BazelServerConnection by lazy { DefaultBazelServerConnection(project) }
}
