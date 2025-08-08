package org.jetbrains.bazel.base

import kotlinx.coroutines.test.runTest
import org.jetbrains.bazel.bazelrunner.outputs.ProcessSpawner
import org.jetbrains.bazel.commons.BidirectionalMap
import org.jetbrains.bazel.commons.EnvironmentProvider
import org.jetbrains.bazel.commons.FileUtil
import org.jetbrains.bazel.commons.SystemInfoProvider
import org.jetbrains.bazel.install.Install
import org.jetbrains.bazel.install.cli.CliOptions
import org.jetbrains.bazel.install.cli.ProjectViewCliOptions
import org.jetbrains.bazel.performance.telemetry.TelemetryManager
import org.jetbrains.bazel.server.connection.startServer
import org.jetbrains.bazel.startup.FileUtilIntellij
import org.jetbrains.bazel.startup.GenericCommandLineProcessSpawner
import org.jetbrains.bazel.startup.IntellijBidirectionalMap
import org.jetbrains.bazel.startup.IntellijEnvironmentProvider
import org.jetbrains.bazel.startup.IntellijSystemInfoProvider
import org.jetbrains.bazel.startup.IntellijTelemetryManager
import org.jetbrains.bsp.protocol.FeatureFlags
import java.nio.file.Paths
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

abstract class BazelBaseTestRunner {
  val basePath = Paths.get("").toAbsolutePath()
  val targets = (System.getenv("TARGETS") ?: "//...").split(",")
  val gazelleTarget: String? = System.getenv("GAZELLE_TARGET")
  val enabledRules = (System.getenv("ENABLED_RULES") ?: "").split(",")
  val bazelBinary = basePath.resolve(System.getenv("BIT_BAZEL_BINARY"))
  val workspaceDir = basePath.resolve(System.getenv("BIT_WORKSPACE_DIR")).parent

  init {
    // Initialize providers for e2e tests
    SystemInfoProvider.provideSystemInfoProvider(IntellijSystemInfoProvider)
    FileUtil.provideFileUtil(FileUtilIntellij)
    EnvironmentProvider.provideEnvironmentProvider(IntellijEnvironmentProvider)
    ProcessSpawner.provideProcessSpawner(GenericCommandLineProcessSpawner)
    TelemetryManager.provideTelemetryManager(IntellijTelemetryManager)
    BidirectionalMap.provideBidirectionalMapFactory { IntellijBidirectionalMap<Any, Any>() }

    installServer()
  }

  private fun installServer() {
    Install.runInstall(
      cliOptions = CliOptions(
        workspaceDir = workspaceDir,
        projectViewCliOptions = projectViewCliOptions(),
      ),
      silent = true,
    )
  }

  protected open fun projectViewCliOptions() = ProjectViewCliOptions(
    bazelBinary = bazelBinary,
    targets = targets,
    gazelleTarget = gazelleTarget,
    enabledRules = enabledRules,
  )

  protected open fun performTest(timeout: Duration = 60.seconds, doTest: suspend (Session) -> Unit) {
    val featureFlags =
      FeatureFlags(
        isPythonSupportEnabled = true,
        isAndroidSupportEnabled = true,
        isGoSupportEnabled = true,
        isPropagateExportsFromDepsEnabled = false,
      )
    val client = MockClient()
    runTest(timeout = timeout) {
      val server = startServer(client, workspaceDir, null, featureFlags)
      val session = Session(client, server)
      doTest(session)
    }
  }

}
