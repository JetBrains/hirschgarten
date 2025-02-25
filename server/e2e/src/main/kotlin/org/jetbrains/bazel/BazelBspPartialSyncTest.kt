package org.jetbrains.bazel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.JvmBuildTarget
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import kotlinx.coroutines.future.await
import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.install.Install
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsPartialParams
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
      testClient.test(3.minutes) { session, _ ->
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
          )
        javaTargetsJavaLibrarySources.roots = listOf("file://\$WORKSPACE/")

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
          JvmBuildTarget().also {
            it.javaHome = javaHome
            it.javaVersion = "11"
          }
        val javaTargetsJavaBinary =
          BuildTarget(
            partialSyncTargetId,
            listOf("application"),
            listOf("java"),
            emptyList(),
            BuildTargetCapabilities().apply {
              canCompile = true
              canTest = false
              canRun = true
              canDebug = false
            },
          ).apply {
            displayName = "$targetPrefix//java_targets:java_binary"
            baseDirectory = "file://\$WORKSPACE/java_targets/"
            dataKind = "jvm"
            data = jvmBuildTarget
          }

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
        javaTargetsJavaBinarySources.roots = listOf("file://\$WORKSPACE/")

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
      JvmBuildTarget().also {
        it.javaHome = javaHome
        it.javaVersion = "11"
      }

    val javaTargetsJavaLibrary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//java_targets:java_library"),
        listOf("library"),
        listOf("java"),
        listOf(),
        BuildTargetCapabilities().apply {
          canCompile = true
          canTest = false
          canRun = false
          canDebug = false
        },
      ).apply {
        displayName = "$targetPrefix//java_targets:java_library"
        baseDirectory = "file://\$WORKSPACE/java_targets/"
        dataKind = "jvm"
        data = jvmBuildTarget
      }

    return WorkspaceBuildTargetsResult(listOf(javaTargetsJavaLibrary))
  }
}
