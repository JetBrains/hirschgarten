package org.jetbrains.bazel.server

import kotlinx.coroutines.runBlocking
import org.jetbrains.bazel.bazelrunner.BazelInfoResolver
import org.jetbrains.bazel.bazelrunner.BazelRunner
import org.jetbrains.bazel.bazelrunner.outputs.ProcessSpawner
import org.jetbrains.bazel.commons.BazelPathsResolver
import org.jetbrains.bazel.commons.BidirectionalMap
import org.jetbrains.bazel.commons.EnvironmentProvider
import org.jetbrains.bazel.commons.FileUtil
import org.jetbrains.bazel.commons.SystemInfoProvider
import org.jetbrains.bazel.install.EnvironmentCreator
import org.jetbrains.bazel.logger.BspClientLogger
import org.jetbrains.bazel.performance.telemetry.TelemetryManager
import org.jetbrains.bazel.server.bsp.info.BspInfo
import org.jetbrains.bazel.server.bsp.managers.BazelBspCompilationManager
import org.jetbrains.bazel.startup.FileUtilIntellij
import org.jetbrains.bazel.startup.GenericCommandLineProcessSpawner
import org.jetbrains.bazel.startup.IntellijBidirectionalMap
import org.jetbrains.bazel.startup.IntellijEnvironmentProvider
import org.jetbrains.bazel.startup.IntellijSystemInfoProvider
import org.jetbrains.bazel.startup.IntellijTelemetryManager
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.JoinedBuildClient
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.system.exitProcess

/**
 * A tool that installs the Bazel BSP server in a project and extracts aspect output paths.
 * This tool runs the server installation process and executes the project resolution
 * up to the point where aspect outputs are generated, then prints their paths to stdout.
 */
object AspectOutputExtractor {
  @JvmStatic
  fun main(args: Array<String>) =
    runBlocking {
      SystemInfoProvider.provideSystemInfoProvider(IntellijSystemInfoProvider)
      FileUtil.provideFileUtil(FileUtilIntellij)
      EnvironmentProvider.provideEnvironmentProvider(IntellijEnvironmentProvider)
      ProcessSpawner.provideProcessSpawner(GenericCommandLineProcessSpawner)
      TelemetryManager.provideTelemetryManager(IntellijTelemetryManager)
      BidirectionalMap.provideBidirectionalMapFactory { IntellijBidirectionalMap<Any, Any>() }

      if (args.size < 2) {
        println("Usage: AspectOutputExtractor <project-path> <project-view-path>")
        println("  project-path: Path to the Bazel project where the server should be installed")
        println("  project-view-path: Path to the project view file to use")
        exitProcess(1)
      }

      val projectPath = Path.of(args[0])
      val projectViewPath = Path.of(args[1])

      if (!projectPath.exists()) {
        println("Error: Project path does not exist: $projectPath")
        exitProcess(1)
      }

      if (!projectViewPath.exists()) {
        println("Error: Project view path does not exist: $projectViewPath")
        exitProcess(1)
      }

      try {
        val aspectPaths = extractAspectOutputPaths(projectPath, projectViewPath)
        println("Aspect output paths:")
        aspectPaths.forEach { path ->
          println(path.toString())
        }
      } catch (e: Exception) {
        println("Error extracting aspect output paths: ${e.message}")
        e.printStackTrace()
        exitProcess(1)
      }
    }

  private suspend fun extractAspectOutputPaths(projectPath: Path, projectViewPath: Path): Set<Path> {
    // Create environment (.bazelbsp directory and files)
    EnvironmentCreator(projectPath).create()

    // Get paths after installation
    val dotBazelBspDir = projectPath.resolve(".bazelbsp")

    // Create a dummy BSP client
    val dummyClient =
      object : JoinedBuildClient {
        override fun onBuildLogMessage(params: org.jetbrains.bsp.protocol.LogMessageParams) {}

        override fun onBuildTaskStart(params: org.jetbrains.bsp.protocol.TaskStartParams) {}

        override fun onBuildTaskFinish(params: org.jetbrains.bsp.protocol.TaskFinishParams) {}

        override fun onBuildPublishDiagnostics(params: org.jetbrains.bsp.protocol.PublishDiagnosticsParams) {}

        override fun onPublishCoverageReport(report: org.jetbrains.bsp.protocol.CoverageReport) {}
      }

    // Create logger
    val bspClientLogger = BspClientLogger(dummyClient)

    // Create initial Bazel runner to resolve bazel info
    val initialBazelRunner = BazelRunner(bspClientLogger, projectPath)

    // Create feature flags with Python support enabled
    val featureFlags =
      FeatureFlags(
        isPythonSupportEnabled = true,
        isGoSupportEnabled = true,
      )

    // TODO: For now, create a minimal WorkspaceContext for aspect extraction
    // In the future, this should parse the actual project view file
    val workspaceContext = createMinimalWorkspaceContext(projectPath, dotBazelBspDir)

    // Create BSP info
    val bspInfo = BspInfo(projectPath)

    // Resolve bazel info with initial runner
    val bazelInfoResolver = BazelInfoResolver(initialBazelRunner)
    val bazelInfo = bazelInfoResolver.resolveBazelInfo(workspaceContext)

    // Now create properly configured Bazel runner with bazelInfo
    val bazelRunner = BazelRunner(bspClientLogger, projectPath, bazelInfo)

    // Create other dependencies
    val bazelPathsResolver = BazelPathsResolver(bazelInfo)
    val compilationManager =
      BazelBspCompilationManager(
        client = dummyClient,
        bazelRunner = bazelRunner,
        workspaceRoot = projectPath,
        bazelPathsResolver = bazelPathsResolver,
      )

    // Create server and project provider
    val bazelBspServer =
      BazelBspServer(
        bspInfo = bspInfo,
        workspaceContext = workspaceContext,
        workspaceRoot = projectPath,
      )

    // Extract aspect output paths using the project resolver
    val projectProvider =
      bazelBspServer.createProjectProvider(
        bspInfo = bspInfo,
        bazelInfo = bazelInfo,
        workspaceContext = workspaceContext,
        featureFlags = featureFlags,
        bazelRunner = bazelRunner,
        bazelPathsResolver = bazelPathsResolver,
        compilationManager = compilationManager,
        bspClientLogger = bspClientLogger,
      )

    return projectProvider.projectResolver.getAspectOutputPaths()
  }

  private fun createMinimalWorkspaceContext(projectPath: Path, dotBazelBspDir: Path): WorkspaceContext =
    WorkspaceContext(
      targets = emptyList(),
      directories = emptyList(),
      buildFlags = emptyList(),
      syncFlags = emptyList(),
      debugFlags = emptyList(),
      bazelBinary = null,
      allowManualTargetsSync = false,
      dotBazelBspDirPath = dotBazelBspDir,
      importDepth = -1,
      enabledRules = emptyList(),
      ideJavaHomeOverride = null,
      shardSync = false,
      targetShardSize = 1000,
      shardingApproach = null,
      importRunConfigurations = emptyList(),
      gazelleTarget = null,
      indexAllFilesInDirectories = false,
      pythonCodeGeneratorRuleNames = emptyList(),
      importIjars = true,
      deriveInstrumentationFilterFromTargets = false,
    )
}
