package org.jetbrains.bsp.bazel

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
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object BazelBspAllowManualTargetsSyncTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-95
  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun additionalServerInstallArguments(): Array<String> = arrayOf("-allow-manual-targets-sync")

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      resolveProject(),
      compareWorkspaceTargetsResults(),
      sourcesResults(),
    )

  private fun resolveProject(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "resolve project",
    ) { testClient.testResolveProject(2.minutes) }

  private fun compareWorkspaceTargetsResults(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "compare workspace targets results",
    ) { testClient.testWorkspaceTargets(30.seconds, expectedWorkspaceBuildTargetsResult()) }

  private fun sourcesResults(): BazelBspTestScenarioStep {
    val manualTargetTestJavaFile =
      SourceItem(
        "file://\$WORKSPACE/manual_target/TestJavaFile.java",
        SourceItemKind.FILE,
        false,
      )
    val manualTargetTestJavaFileSources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//manual_target:java_library"),
        listOf(manualTargetTestJavaFile),
      )
    manualTargetTestJavaFileSources.roots = listOf("file://\$WORKSPACE/")

    val manualTargetTestJavaTest =
      SourceItem("file://\$WORKSPACE/manual_target/JavaTest.java", SourceItemKind.FILE, false)
    val manualTargetTestJavaTestSources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//manual_target:java_test"),
        listOf(manualTargetTestJavaTest),
      )
    manualTargetTestJavaTestSources.roots = listOf("file://\$WORKSPACE/")

    val manualTargetTestJavaBinary =
      SourceItem(
        "file://\$WORKSPACE/manual_target/TestJavaBinary.java",
        SourceItemKind.FILE,
        false,
      )
    val manualTargetTestJavaBinarySources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//manual_target:java_binary"),
        listOf(manualTargetTestJavaBinary),
      )
    manualTargetTestJavaBinarySources.roots = listOf("file://\$WORKSPACE/")

    val sourcesParams = SourcesParams(expectedTargetIdentifiers())
    val expectedSourcesResult =
      SourcesResult(
        listOfNotNull(
          manualTargetTestJavaFileSources,
          manualTargetTestJavaBinarySources,
          manualTargetTestJavaTestSources,
        ),
      )
    return BazelBspTestScenarioStep("sources results") {
      testClient.testSources(30.seconds, sourcesParams, expectedSourcesResult)
    }
  }

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val architecturePart = if (System.getProperty("os.arch") == "aarch64") "_aarch64" else ""
    val javaHomeBazel5And6 = "file://\$BAZEL_OUTPUT_BASE_PATH/external/remotejdk11_\$OS$architecturePart/"
    val javaHomeBazel7 =
      "file://\$BAZEL_OUTPUT_BASE_PATH/external/" +
        "rules_java${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}toolchains${bzlmodRepoNameSeparator}remotejdk11_\$OS$architecturePart/"
    val javaHome = if (isBzlmod) javaHomeBazel7 else javaHomeBazel5And6
    val jvmBuildTarget =
      JvmBuildTarget().also {
        it.javaHome = javaHome
        it.javaVersion = "11"
      }

    val manualTargetJavaLibrary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//manual_target:java_library"),
        listOf("library"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities().apply {
          canCompile = true
          canTest = false
          canRun = false
          canDebug = false
        },
      ).apply {
        displayName = "$targetPrefix//manual_target:java_library"
        baseDirectory = "file://\$WORKSPACE/manual_target/"
        dataKind = "jvm"
        data = jvmBuildTarget
      }

    val manualTargetJavaBinary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//manual_target:java_binary"),
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
        displayName = "$targetPrefix//manual_target:java_binary"
        baseDirectory = "file://\$WORKSPACE/manual_target/"
        dataKind = "jvm"
        data = jvmBuildTarget
      }

    val manualTargetJavaTest =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//manual_target:java_test"),
        listOf("test"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities().apply {
          canCompile = true
          canTest = true
          canRun = false
          canDebug = false
        },
      ).apply {
        displayName = "$targetPrefix//manual_target:java_test"
        baseDirectory = "file://\$WORKSPACE/manual_target/"
        dataKind = "jvm"
        data = jvmBuildTarget
      }

    return WorkspaceBuildTargetsResult(
      listOfNotNull(
        manualTargetJavaLibrary,
        manualTargetJavaBinary,
        manualTargetJavaTest,
      ),
    )
  }
}
