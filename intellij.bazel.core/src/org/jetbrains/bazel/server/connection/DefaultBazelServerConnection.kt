package org.jetbrains.bazel.server.connection

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.bazelrunner.BazelInfoResolver
import org.jetbrains.bazel.bazelrunner.BazelProcessLauncherProvider
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.install.EnvironmentCreator
import org.jetbrains.bazel.languages.bazelversion.psi.BazelVersionLiteral
import org.jetbrains.bazel.languages.bazelversion.service.BazelVersionWorkspaceResolver
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.projectView
import org.jetbrains.bazel.server.BazelServerConnection
import org.jetbrains.bazel.server.BazelServerFacade
import org.jetbrains.bazel.server.BazelServerService
import org.jetbrains.bazel.server.bsp.BazelServerFacadeImpl
import org.jetbrains.bazel.server.bsp.managers.BazelBspAspectsManager
import org.jetbrains.bazel.server.sync.BspProjectMapper
import org.jetbrains.bazel.server.sync.ExecuteService
import org.jetbrains.bazel.server.sync.ProjectResolver
import org.jetbrains.bazel.server.sync.firstPhase.FirstPhaseProjectResolver
import org.jetbrains.bazel.sync.BazelEnvironmentService
import org.jetbrains.bazel.sync.BazelOutFileHardLinks
import org.jetbrains.bazel.sync.environment.BazelApplicationContextService
import org.jetbrains.bazel.sync.workspace.mapper.normal.DefaultBazelOutputFileHardLinks
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bsp.protocol.TaskId
import java.util.concurrent.atomic.AtomicReference

internal class DefaultBazelServerConnection(private val project: Project) : BazelServerConnection {
  private val workspaceRoot = project.rootDir.toNioPath()
  private val environmentCreator = EnvironmentCreator(workspaceRoot).also {
    it.create()
  }

  data class ServerWithVersionLiteral(val server: BazelServerFacadeImpl?, val versionLiteral: BazelVersionLiteral?)

  private val serverAndVersionLiteral = AtomicReference<ServerWithVersionLiteral>(ServerWithVersionLiteral(null, null))

  override suspend fun <T> runWithServer(taskId: TaskId?, task: suspend (server: BazelServerFacade) -> T): T {
    return task(getServer(taskId))
  }

  private suspend fun getServer(taskId: TaskId?): BazelServerFacadeImpl {
    // ensure `.bazelbsp` directory exists and functions
    environmentCreator.create()

    val projectView = project.projectView()
    var (server, oldVersionLiteral) = this.serverAndVersionLiteral.get()
    val projectPath = project.rootDir.toNioPath()
    val resolvedVersion = BazelVersionWorkspaceResolver.resolveBazelVersionFromWorkspace(projectPath)
    val bazelVersionUpdated = oldVersionLiteral != resolvedVersion

    if (server == null ||
        bazelVersionUpdated ||
        server.projectView != projectView) {
      server = createServer(projectView, taskId)
      this.serverAndVersionLiteral.set(ServerWithVersionLiteral(server, resolvedVersion))
    }
    return server
  }

  private suspend fun createServer(projectView: ProjectView, taskId: TaskId?): BazelServerFacadeImpl {
    val taskEventsHandler = BazelTaskEventsService.getInstance(project)
    val bazelInfoResolver = BazelInfoResolver(workspaceRoot)
    val bazelProcessLauncherProvider = BazelProcessLauncherProvider.getInstance()
    val bazelProcessLauncher =
      bazelProcessLauncherProvider.createBazelProcessLauncher(workspaceRoot, BazelEnvironmentService.getInstance(project).getEnvironment())
    val bazelRunner = BazelRunner.create(project, taskEventsHandler, workspaceRoot, bazelProcessLauncher, projectView)

    val bazelInfo = bazelInfoResolver.resolveBazelInfo(bazelRunner, projectView, taskId)
    val bazelPathsResolver = BazelPathsResolver(bazelInfo)

    val outFileHardLinks =
      if (BazelFeatureFlags.hardLinkOutputFiles &&
          !service<BazelApplicationContextService>().disableHardLinksOutputFiles)
        DefaultBazelOutputFileHardLinks(project, bazelInfo)
      else
        BazelOutFileHardLinks.NONE

    val executeService =
      ExecuteService(
        project = project,
        workspaceRoot = workspaceRoot,
        taskEventsHandler = taskEventsHandler,
        bazelRunner = bazelRunner,
        projectView = projectView,
        bazelPathsResolver = bazelPathsResolver,
      )

    val bazelBspAspectsManager =
      BazelBspAspectsManager(
        workspaceRoot = workspaceRoot,
        executeService = executeService,
        bazelRelease = bazelInfo.release,
      )

    val projectResolver =
      ProjectResolver(
        bazelBspAspectsManager = bazelBspAspectsManager,
        projectView = projectView,
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
        projectView = projectView,
        bazelInfo = bazelInfo,
        taskEventsHandler = taskEventsHandler,
      )

    val bspProjectMapper =
      BspProjectMapper(
        workspaceRoot = workspaceRoot,
        bazelRunner = bazelRunner,
        projectView = projectView,
        bazelPathsResolver = bazelPathsResolver,
      )

    return BazelServerFacadeImpl(
      bspMapper = bspProjectMapper,
      projectResolver = projectResolver,
      firstPhaseProjectResolver = firstPhaseProjectResolver,
      executeService = executeService,
      projectView = projectView,
      bazelInfo = bazelInfo,
      bazelPathsResolver = bazelPathsResolver,
      outFileHardLinks = outFileHardLinks,
    )
  }
}

internal class BazelServerServiceImpl(project: Project) : BazelServerService {
  override val connection: BazelServerConnection by lazy { DefaultBazelServerConnection(project) }
}
