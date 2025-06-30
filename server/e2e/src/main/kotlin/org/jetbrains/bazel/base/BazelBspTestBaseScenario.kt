package org.jetbrains.bazel.base

import com.intellij.openapi.util.SystemInfo
import org.jetbrains.bazel.commons.EnvironmentProvider
import org.jetbrains.bazel.commons.FileUtil
import org.jetbrains.bazel.commons.SystemInfoProvider
import org.jetbrains.bazel.install.Install
import org.jetbrains.bazel.install.cli.CliOptions
import org.jetbrains.bazel.install.cli.ProjectViewCliOptions
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.FeatureFlags
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.testkit.client.TestClient
import org.jetbrains.bsp.testkit.client.bazel.BazelJsonTransformer
import java.io.File
import java.nio.file.Path
import java.nio.file.Path.of
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.system.exitProcess

/** Test implementation of SystemInfoProvider */
private class TestSystemInfoProvider : SystemInfoProvider {
  override val isWindows: Boolean = SystemInfo.isWindows
  override val isMac: Boolean = SystemInfo.isMac
  override val isLinux: Boolean = SystemInfo.isLinux
  override val isAarch64: Boolean = SystemInfo.isAarch64
}

/** Test implementation of FileUtil */
private class TestFileUtil : FileUtil {
  override fun isAncestor(ancestor: String, file: String, strict: Boolean): Boolean {
    val ancestorPath = Path.of(ancestor).toAbsolutePath().normalize()
    val filePath = Path.of(file).toAbsolutePath().normalize()

    if (strict && ancestorPath == filePath) {
      return false
    }

    return filePath.startsWith(ancestorPath)
  }

  override fun isAncestor(ancestor: File, file: File, strict: Boolean): Boolean {
    return isAncestor(ancestor.absolutePath, file.absolutePath, strict)
  }
}

/** Test implementation of EnvironmentProvider */
private class TestEnvironmentProvider : EnvironmentProvider {
  override fun getValue(name: String): String? {
    return System.getenv(name)
  }
}

abstract class BazelBspTestBaseScenario {
  protected val bazelBinary = System.getenv("BIT_BAZEL_BINARY")
  protected val workspaceDir = System.getenv("BIT_WORKSPACE_DIR")

  val majorBazelVersion: Int = calculateMajorBazelVersion()
  val targetPrefix = "@"

  val isBzlmod = majorBazelVersion >= 7
  val bzlmodRepoNameSeparator = if (majorBazelVersion == 7) "~" else "+"

  private val architecturePart
    get() = if (System.getProperty("os.arch") == "aarch64") "_aarch64" else ""
  val javaHomeArchitecture get() = "\$OS$architecturePart"

  private val bazelArch
    get() =
      if (SystemInfo.isMac) {
        "darwin_arm64"
      } else {
        "k8"
      }
  private val mainBinName = if (majorBazelVersion >= 7) "_main" else "__main__"
  val bazelBinDirectory get() = "\$BAZEL_OUTPUT_BASE_PATH/execroot/$mainBinName/bazel-out/$bazelArch-fastbuild/bin"

  init {
    // Initialize providers for e2e tests
    SystemInfoProvider.provideSystemInfoProvider(TestSystemInfoProvider())
    FileUtil.provideFileUtil(TestFileUtil())
    EnvironmentProvider.provideEnvironmentProvider(TestEnvironmentProvider())

    installServer()
  }

  private fun calculateMajorBazelVersion(): Int {
    val dirName = Path(bazelBinary).parent.name
    // With bzlmod enabled the directory name is something like:
    // rules_bazel_integration_test~0.18.0~bazel_binaries~build_bazel_bazel_6_3_2
    val bazelPart =
      if (dirName.contains("~")) {
        dirName.split("~")[3]
      } else if (dirName.contains("+")) {
        dirName.split("+")[3]
      } else {
        dirName
      }
    return bazelPart.split("_")[3].toIntOrNull() ?: 100
  }

  protected open fun installServer() {
    Install.runInstall(
      CliOptions(
        workspaceDir = Path(workspaceDir),
        projectViewCliOptions =
          ProjectViewCliOptions(
            bazelBinary = Path(bazelBinary),
            targets = listOf("//..."),
          ),
      ),
    )
  }

  private fun processBazelOutput(vararg args: String): String {
    val command = arrayOf<String>(bazelBinary, *args)
    val process = ProcessBuilder(*command).directory(Path(workspaceDir).toFile()).start()
    val output =
      process.inputStream
        .bufferedReader()
        .readText()
        .trim()
    val exitCode = process.waitFor()
    if (exitCode != 0) {
      val error =
        process.errorStream
          .bufferedReader()
          .readText()
          .trim()
      throw RuntimeException("Command '${command.joinToString(" ")}' failed with exit code $exitCode.\n$error")
    }
    return output
  }

  /**
   * Bazelisk often fails to download Bazel on TeamCity because of network issues. Retry to reduce test flakiness.
   */
  private fun processBazelOutputWithDownloadRetry(vararg args: String): String {
    var delayMs = 1000L
    repeat(3) {
      try {
        return processBazelOutput(*args)
      } catch (e: RuntimeException) {
        val message = e.message
        if (message == null || "could not download" !in message) {
          throw e
        }
        println("WARN: Failed to download Bazel. Retrying in $delayMs ms.")
        Thread.sleep(delayMs)
        delayMs *= 2
      }
    }
    return processBazelOutput(*args)
  }

  fun executeScenario() {
    println("Running scenario...")
    val scenarioStepsExecutionResult: Boolean?
    try {
      scenarioStepsExecutionResult = executeScenarioSteps()
      println("Running scenario done.")
    } finally {
      val logFile = Path(workspaceDir).resolve("all.log").toFile()
      if (logFile.exists()) {
        // Because we are in a sandbox there's no easy way to get the log file content after the test run - so we println it here.
        println("Log file content:\n${logFile.readText()}")
      } else {
        println("WARN: Log file not found.")
      }
    }

    if (scenarioStepsExecutionResult == true) {
      println("Test passed")
      exitProcess(SUCCESS_EXIT_CODE)
    } else {
      println("Test failed!")
      exitProcess(FAIL_EXIT_CODE)
    }
  }

  protected fun createTestkitClient(): TestClient {
    println("Testing repo workspace path: $workspaceDir")
    println("Creating TestClient...")
    val featureFlags =
      FeatureFlags(
        isPythonSupportEnabled = true,
        isAndroidSupportEnabled = true,
        isGoSupportEnabled = true,
        isPropagateExportsFromDepsEnabled = false,
      )
    val bazelCache = Path(processBazelOutputWithDownloadRetry("info", "execution_root"))
    val bazelOutputBase = Path(processBazelOutput("info", "output_base"))
    val bazelJsonTransformer =
      BazelJsonTransformer(
        of(workspaceDir),
        bazelCache,
        bazelOutputBase,
      )

    return TestClient(
      Path.of(workspaceDir),
      { s: String -> bazelJsonTransformer.transformJson(s) },
      featureFlags,
    ).also { println("Created TestClient done.") }
  }

  private fun executeScenarioSteps(): Boolean = scenarioSteps().map { it.executeAndReturnResult() }.all { it }

  protected fun expectedTargetIdentifiers(): List<Label> = expectedWorkspaceBuildTargetsResult().targets.map { it.id }

  protected abstract fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult

  protected abstract fun scenarioSteps(): List<BazelBspTestScenarioStep>

  companion object {
    private const val SUCCESS_EXIT_CODE = 0
    private const val FAIL_EXIT_CODE = 1
  }
}
