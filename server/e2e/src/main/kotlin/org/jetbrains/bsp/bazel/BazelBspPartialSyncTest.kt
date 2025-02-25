package org.jetbrains.bsp.bazel

import kotlinx.coroutines.future.await
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bsp.bazel.install.Install
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.SourceItemKind
import org.jetbrains.bsp.protocol.SourcesItem
import org.jetbrains.bsp.protocol.SourcesParams
import org.jetbrains.bsp.protocol.SourcesResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsPartialParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import kotlin.time.Duration.Companion.minutes

object BazelBspPartialSyncTest : BazelBspTestBaseScenario() {
  private val testClient = createBazelClient()

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-95
  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun installServer() {
    Install.main(
      arrayOf(
        "-d",
        workspaceDir,
        "-b",
        bazelBinary,
        "-t",
        "@//java_targets:java_library",
        *additionalServerInstallArguments(),
      ),
    )
  }

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> = listOf(runInitialSyncAndPartialSync())

  private fun runInitialSyncAndPartialSync(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep("should do an initial sync on 1 target and then partial sync on another target") {
      testClient.test(3.minutes) { session ->
        // initial sync
        val workspaceBuildTargetsResult = session.server.workspaceBuildTargets().await()
        testClient.assertJsonEquals(expectedWorkspaceBuildTargetsResult(), workspaceBuildTargetsResult)

        val javaTargetsJavaLibraryJava =
          SourceItem(
            "file://\$WORKSPACE/java_targets/JavaLibrary.java",
            SourceItemKind.FILE,
            false,
          )
        val javaTargetsJavaLibrarySources =
          SourcesItem(
            BuildTargetIdentifier("$targetPrefix//java_targets:java_library"),
            listOf(javaTargetsJavaLibraryJava),
            roots = listOf("file://\$WORKSPACE/"),
          )

        val sourcesParams = SourcesParams(expectedTargetIdentifiers())
        val expectedSourcesResult = SourcesResult(listOf(javaTargetsJavaLibrarySources))
        val buildTargetSourcesResult = session.server.buildTargetSources(sourcesParams).await()
        testClient.assertJsonEquals(expectedSourcesResult, buildTargetSourcesResult)

        // partial sync
        val partialSyncTargetId = BuildTargetIdentifier("$targetPrefix//java_targets:java_binary")
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
              canDebug = false,
            ),
            displayName = "$targetPrefix//java_targets:java_binary",
            baseDirectory = "file://\$WORKSPACE/java_targets/",
            data = jvmBuildTarget,
          )

        val workspaceBuildTargetsPartialParams =
          WorkspaceBuildTargetsPartialParams(listOf(BuildTargetIdentifier("$targetPrefix//java_targets:java_binary")))
        val expectedTargetsResult = WorkspaceBuildTargetsResult(listOf(javaTargetsJavaBinary))

        val workspaceBuildTargetsPartialResult = session.server.workspaceBuildTargetsPartial(workspaceBuildTargetsPartialParams).await()
        testClient.assertJsonEquals(expectedTargetsResult, workspaceBuildTargetsPartialResult)

        val javaTargetsJavaBinaryJava =
          SourceItem(
            "file://\$WORKSPACE/java_targets/JavaBinary.java",
            SourceItemKind.FILE,
            false,
          )
        val javaTargetsJavaBinarySources =
          SourcesItem(
            partialSyncTargetId,
            listOf(javaTargetsJavaBinaryJava),
          )

        val partialSyncSourcesParams = SourcesParams(listOf(partialSyncTargetId))
        val expectedPartialSyncSourcesResult = SourcesResult(listOf(javaTargetsJavaBinarySources))
        val result = session.server.buildTargetSources(partialSyncSourcesParams).await()
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
        BuildTargetIdentifier("$targetPrefix//java_targets:java_library"),
        listOf("library"),
        listOf("java"),
        listOf(),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = false,
          canDebug = false,
        ),
        displayName = "$targetPrefix//java_targets:java_library",
        baseDirectory = "file://\$WORKSPACE/java_targets/",
        data = jvmBuildTarget,
      )

    return WorkspaceBuildTargetsResult(listOf(javaTargetsJavaLibrary))
  }
}
