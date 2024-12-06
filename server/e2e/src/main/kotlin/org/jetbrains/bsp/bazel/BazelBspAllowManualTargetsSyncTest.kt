package org.jetbrains.bsp.bazel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.JvmBuildTarget
import ch.epfl.scala.bsp4j.ScalaBuildTarget
import ch.epfl.scala.bsp4j.ScalaPlatform
import ch.epfl.scala.bsp4j.SourceItem
import ch.epfl.scala.bsp4j.SourceItemKind
import ch.epfl.scala.bsp4j.SourcesItem
import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.SourcesResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bsp.bazel.server.model.Label
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

    val manualTargetTestScalaFile =
      SourceItem(
        "file://\$WORKSPACE/manual_target/TestScalaFile.scala",
        SourceItemKind.FILE,
        false,
      )
    val manualTargetTestScalaFileSources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//manual_target:scala_library"),
        listOf(manualTargetTestScalaFile),
      )
    manualTargetTestScalaFileSources.roots = listOf("file://\$WORKSPACE/")

    val manualTargetTestJavaTest =
      SourceItem("file://\$WORKSPACE/manual_target/JavaTest.java", SourceItemKind.FILE, false)
    val manualTargetTestJavaTestSources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//manual_target:java_test"),
        listOf(manualTargetTestJavaTest),
      )
    manualTargetTestJavaTestSources.roots = listOf("file://\$WORKSPACE/")

    val manualTargetTestScalaTest =
      SourceItem(
        "file://\$WORKSPACE/manual_target/ScalaTest.scala",
        SourceItemKind.FILE,
        false,
      )
    val manualTargetTestScalaTestSources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//manual_target:scala_test"),
        listOf(manualTargetTestScalaTest),
      )
    manualTargetTestScalaTestSources.roots = listOf("file://\$WORKSPACE/")

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

    val manualTargetTestScalaBinary =
      SourceItem(
        "file://\$WORKSPACE/manual_target/TestScalaBinary.scala",
        SourceItemKind.FILE,
        false,
      )
    val manualTargetTestScalaBinarySources =
      SourcesItem(
        BuildTargetIdentifier("$targetPrefix//manual_target:scala_binary"),
        listOf(manualTargetTestScalaBinary),
      )
    manualTargetTestScalaBinarySources.roots = listOf("file://\$WORKSPACE/")

    val sourcesParams = SourcesParams(expectedTargetIdentifiers())
    val expectedSourcesResult =
      SourcesResult(
        listOfNotNull(
          manualTargetTestJavaFileSources,
          manualTargetTestScalaFileSources,
          manualTargetTestJavaBinarySources,
          manualTargetTestScalaBinarySources,
          manualTargetTestJavaTestSources,
          manualTargetTestScalaTestSources,
        ),
      )
    return BazelBspTestScenarioStep("sources results") {
      testClient.testSources(30.seconds, sourcesParams, expectedSourcesResult)
    }
  }

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val javaHome = "file://\$BAZEL_OUTPUT_BASE_PATH/external/remotejdk11_$javaHomeArchitecture/"
    val jvmBuildTarget =
      JvmBuildTarget().also {
        it.javaHome = javaHome
        it.javaVersion = "11"
      }

    val scalaBuildTarget =
      ScalaBuildTarget(
        "org.scala-lang",
        "2.12.14",
        "2.12",
        ScalaPlatform.JVM,
        listOf(
          "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_compiler/scala-compiler-2.12.14.jar",
          "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_library/scala-library-2.12.14.jar",
          "file://\$BAZEL_OUTPUT_BASE_PATH/external/io_bazel_rules_scala_scala_reflect/scala-reflect-2.12.14.jar",
        ),
      )
    scalaBuildTarget.jvmBuildTarget = jvmBuildTarget

    val manualTargetScalaLibrary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//manual_target:scala_library"),
        listOf("library"),
        listOf("scala"),
        listOf(
          BuildTargetIdentifier(Label.synthetic("scala-compiler-2.12.14.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scala-library-2.12.14.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scala-reflect-2.12.14.jar").toString()),
        ),
        BuildTargetCapabilities().apply {
          canCompile = true
          canTest = false
          canRun = false
          canDebug = false
        },
      ).apply {
        displayName = "$targetPrefix//manual_target:scala_library"
        baseDirectory = "file://\$WORKSPACE/manual_target/"
        dataKind = "scala"
        data = scalaBuildTarget
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

    val manualTargetScalaBinary =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//manual_target:scala_binary"),
        listOf("application"),
        listOf("scala"),
        listOf(
          BuildTargetIdentifier(Label.synthetic("scala-compiler-2.12.14.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scala-library-2.12.14.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scala-reflect-2.12.14.jar").toString()),
        ),
        BuildTargetCapabilities().apply {
          canCompile = true
          canTest = false
          canRun = true
          canDebug = false
        },
      ).apply {
        displayName = "$targetPrefix//manual_target:scala_binary"
        baseDirectory = "file://\$WORKSPACE/manual_target/"
        dataKind = "scala"
        data = scalaBuildTarget
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

    val manualTargetScalaTest =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//manual_target:scala_test"),
        listOf("test"),
        listOf("scala"),
        listOf(
          BuildTargetIdentifier(Label.synthetic("scala-compiler-2.12.14.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scala-library-2.12.14.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scala-reflect-2.12.14.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("librunner.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalactic_2.12-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-compatible-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-core_2.12-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-featurespec_2.12-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-flatspec_2.12-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-freespec_2.12-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-funspec_2.12-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-funsuite_2.12-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-matchers-core_2.12-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-mustmatchers_2.12-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest-shouldmatchers_2.12-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("scalatest_2.12-3.2.9.jar").toString()),
          BuildTargetIdentifier(Label.synthetic("test_reporter.jar").toString()),
        ),
        BuildTargetCapabilities().apply {
          canCompile = true
          canTest = true
          canRun = false
          canDebug = false
        },
      ).apply {
        displayName = "$targetPrefix//manual_target:scala_test"
        baseDirectory = "file://\$WORKSPACE/manual_target/"
        dataKind = "scala"
        data = scalaBuildTarget
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
        manualTargetScalaLibrary,
        manualTargetJavaBinary,
        manualTargetScalaBinary,
        manualTargetJavaTest,
        manualTargetScalaTest,
      ),
    )
  }
}
