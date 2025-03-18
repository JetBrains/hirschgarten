package org.jetbrains.bazel

import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.install.Install
import org.jetbrains.bazel.install.cli.CliOptions
import org.jetbrains.bazel.install.cli.ProjectViewCliOptions
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.SourcesItem
import org.jetbrains.bsp.protocol.SourcesParams
import org.jetbrains.bsp.protocol.SourcesResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsPartialParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

object BazelBspPartialSyncTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-95
  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun installServer() {
    Install.runInstall(
      CliOptions(
        workspaceDir = Path(workspaceDir),
        projectViewCliOptions =
          ProjectViewCliOptions(
            bazelBinary = Path(bazelBinary),
            targets = listOf("@//java_targets:java_library"),
            directories = listOf(workspaceDir),
          ),
      ),
    )
  }

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> = listOf(runInitialSyncAndPartialSync())

  private fun runInitialSyncAndPartialSync(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep("should do an initial sync on 1 target and then partial sync on another target") {
      testClient.test(3.minutes) { session ->
        // initial sync
        val workspaceBuildTargetsResult = session.server.workspaceBuildTargets()
        testClient.assertJsonEquals(expectedWorkspaceBuildTargetsResult(), workspaceBuildTargetsResult)

        val javaTargetsJavaLibraryJava =
          SourceItem(
            "file://\$WORKSPACE/java_targets/JavaLibrary.java",
            false,
            jvmPackagePrefix = "java_targets",
          )

        val javaTargetsJavaLibrarySources =
          SourcesItem(
            Label.parse("$targetPrefix//java_targets:java_library"),
            listOf(javaTargetsJavaLibraryJava),
          )

        val sourcesParams = SourcesParams(expectedTargetIdentifiers())
        val expectedSourcesResult = SourcesResult(listOf(javaTargetsJavaLibrarySources))
        val buildTargetSourcesResult = session.server.buildTargetSources(sourcesParams)
        testClient.assertJsonEquals(expectedSourcesResult, buildTargetSourcesResult)

        // partial sync
        val partialSyncTargetId = Label.parse("$targetPrefix//java_targets:java_binary")
        val architecturePart = if (System.getProperty("os.arch") == "aarch64") "_aarch64" else ""
        val javaHomeBazel5And6 = "file://\$BAZEL_OUTPUT_BASE_PATH/external/remotejdk11_\$OS$architecturePart/"
        val javaHomeBazel7 =
          "file://\$BAZEL_OUTPUT_BASE_PATH/external/rules_java" +
            "${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}toolchains${bzlmodRepoNameSeparator}remotejdk11_\$OS$architecturePart/"
        val javaHome = if (isBzlmod) javaHomeBazel7 else javaHomeBazel5And6
        val jvmBuildTarget =
          JvmBuildTarget(
            javaHome = javaHome,
            javaVersion = "11",
          )
        val javaTargetsJavaBinary =
          BuildTarget(
            partialSyncTargetId,
            listOf("application"),
            listOf("java"),
            emptyList(),
            BuildTargetCapabilities(
              canCompile = true,
              canTest = false,
              canRun = true,
              
            ),
            displayName = "//java_targets:java_binary",
            baseDirectory = "file://\$WORKSPACE/java_targets/",
            data = jvmBuildTarget,
          )

        val workspaceBuildTargetsPartialParams =
          WorkspaceBuildTargetsPartialParams(listOf(Label.parse("$targetPrefix//java_targets:java_binary")))
        val expectedTargetsResult = WorkspaceBuildTargetsResult(listOf(javaTargetsJavaBinary))

        val workspaceBuildTargetsPartialResult = session.server.workspaceBuildTargetsPartial(workspaceBuildTargetsPartialParams)
        testClient.assertJsonEquals(expectedTargetsResult, workspaceBuildTargetsPartialResult)

        val javaTargetsJavaBinaryJava =
          SourceItem(
            "file://\$WORKSPACE/java_targets/JavaBinary.java",
            false,
            jvmPackagePrefix = "java_targets",
          )
        val javaTargetsJavaBinarySources =
          SourcesItem(
            partialSyncTargetId,
            listOf(javaTargetsJavaBinaryJava),
          )

        val partialSyncSourcesParams = SourcesParams(listOf(partialSyncTargetId))
        val expectedPartialSyncSourcesResult = SourcesResult(listOf(javaTargetsJavaBinarySources))
        val result = session.server.buildTargetSources(partialSyncSourcesParams)
        testClient.assertJsonEquals(expectedPartialSyncSourcesResult, result)
      }
    }

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val architecturePart = if (System.getProperty("os.arch") == "aarch64") "_aarch64" else ""
    val javaHomeBazel5And6 = "file://\$BAZEL_OUTPUT_BASE_PATH/external/remotejdk11_\$OS$architecturePart/"
    val javaHomeBazel7 =
      "file://\$BAZEL_OUTPUT_BASE_PATH/external/rules_java" +
        "${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}toolchains${bzlmodRepoNameSeparator}remotejdk11_\$OS$architecturePart/"
    val javaHome = if (isBzlmod) javaHomeBazel7 else javaHomeBazel5And6
    val jvmBuildTarget =
      JvmBuildTarget(
        javaHome = javaHome,
        javaVersion = "11",
      )

    val javaTargetsJavaLibrary =
      BuildTarget(
        Label.parse("$targetPrefix//java_targets:java_library"),
        listOf("library"),
        listOf("java"),
        listOf(),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = false,
          
        ),
        displayName = "//java_targets:java_library",
        baseDirectory = "file://\$WORKSPACE/java_targets/",
        data = jvmBuildTarget,
      )

    return WorkspaceBuildTargetsResult(listOf(javaTargetsJavaLibrary))
  }
}
