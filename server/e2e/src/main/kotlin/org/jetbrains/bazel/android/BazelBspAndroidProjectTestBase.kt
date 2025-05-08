package org.jetbrains.bazel.android

import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.install.Install
import org.jetbrains.bazel.install.cli.CliOptions
import org.jetbrains.bazel.install.cli.ProjectViewCliOptions
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.time.Duration.Companion.minutes

abstract class BazelBspAndroidProjectTestBase : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()

  protected abstract val enabledRules: List<String>

  override fun installServer() {
    Install.runInstall(
      CliOptions(
        workspaceDir = Path(workspaceDir),
        projectViewCliOptions =
          ProjectViewCliOptions(
            bazelBinary = Path(bazelBinary),
            targets = listOf("//..."),
            enabledRules = enabledRules,
            buildFlags = listOf("--action_env=ANDROID_HOME=${AndroidSdkDownloader.androidSdkPath}"),
          ),
      ),
    )
  }

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      downloadAndroidSdk(),
      compareWorkspaceBuildTargets(),
      compareWorkspaceLibraries(),
    )

  private fun downloadAndroidSdk() =
    BazelBspTestScenarioStep("Download Android SDK") {
      AndroidSdkDownloader.downloadAndroidSdkIfNeeded()
    }

  private fun compareWorkspaceBuildTargets(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "Compare workspace/buildTargets",
    ) {
      testClient.test(timeout = 5.minutes) { session ->
        val result = session.server.workspaceBuildTargets()
        testClient.assertJsonEquals<WorkspaceBuildTargetsResult>(expectedWorkspaceBuildTargetsResult(), result)
      }
    }

  private fun compareWorkspaceLibraries(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "Compare workspace/libraries",
    ) {
      testClient.test(timeout = 5.minutes) { session ->
        // Make sure Bazel unpacks all the dependent AARs
        session.server.runSync(true, "originId")
        val result = session.server.workspaceLibraries()
        val appCompatLibrary = result.libraries.first { "androidx_appcompat_appcompat" in it.id.toString() }

        val jars = appCompatLibrary.jars.toList()
        for (jar in jars) {
          require(jar.exists()) { "Jar $jar should exist" }
        }

        val expectedJarNames = setOf("classes_and_libs_merged.jar", "AndroidManifest.xml", "res", "R.txt")
        val jarNames = jars.map { it.name }.toSet()
        require(jarNames == expectedJarNames) { "$jarNames should be equal to $expectedJarNames" }
      }
    }
}
