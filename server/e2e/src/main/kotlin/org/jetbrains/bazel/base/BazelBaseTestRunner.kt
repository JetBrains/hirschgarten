package org.jetbrains.bazel.base

import org.jetbrains.bazel.bazelrunner.outputs.ProcessSpawner
import org.jetbrains.bazel.commons.BidirectionalMap
import org.jetbrains.bazel.commons.EnvironmentProvider
import org.jetbrains.bazel.commons.FileUtil
import org.jetbrains.bazel.commons.SystemInfoProvider
import org.jetbrains.bazel.install.Install
import org.jetbrains.bazel.install.cli.CliOptions
import org.jetbrains.bazel.install.cli.ProjectViewCliOptions
import org.jetbrains.bazel.performance.telemetry.TelemetryManager
import org.jetbrains.bazel.startup.FileUtilIntellij
import org.jetbrains.bazel.startup.GenericCommandLineProcessSpawner
import org.jetbrains.bazel.startup.IntellijBidirectionalMap
import org.jetbrains.bazel.startup.IntellijEnvironmentProvider
import org.jetbrains.bazel.startup.IntellijSystemInfoProvider
import org.jetbrains.bazel.startup.IntellijTelemetryManager
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.testkit.client.TestClient
import java.nio.file.Paths


abstract class BazelBaseTestRunner {
  val basePath = Paths.get("").toAbsolutePath()
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

  protected open fun installServer() {
    Install.runInstall(
      cliOptions = CliOptions(
        workspaceDir = workspaceDir,
        projectViewCliOptions =
          ProjectViewCliOptions(
            bazelBinary = bazelBinary,
            targets = listOf("//..."),
          ),
      ),
      silent = true,
    )
  }

  protected fun createTestkitClient(): TestClient {
    val featureFlags =
      FeatureFlags(
        isPythonSupportEnabled = true,
        isAndroidSupportEnabled = true,
        isGoSupportEnabled = true,
        isPropagateExportsFromDepsEnabled = false,
      )

    return TestClient(
      workspaceDir,
      { s: String -> s },
      featureFlags,
    )
  }

  companion object {
    private const val SUCCESS_EXIT_CODE = 0
    private const val FAIL_EXIT_CODE = 1
  }
}
