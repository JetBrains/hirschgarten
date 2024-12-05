package org.jetbrains.bsp.bazel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.JvmBuildTarget
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bsp.bazel.install.Install
import org.jetbrains.bsp.bazel.server.model.Label
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import kotlin.time.Duration.Companion.seconds

object BazelBspKotlinProjectTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()

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
        "//...",
        "--enabled-rules",
        "rules_kotlin",
      ),
    )
  }

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      compareWorkspaceTargetsResults(),
    )

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val jvmBuildTargetData =
      JvmBuildTarget().also {
        it.javaHome = "file://\$BAZEL_OUTPUT_BASE_PATH/external/remotejdk11_$javaHomeArchitecture/"
        it.javaVersion = "11"
      }

    val kotlinBuildTargetData =
      KotlinBuildTarget(
        languageVersion = "1.9",
        apiVersion = "1.9",
        kotlincOptions =
          listOf(
            "-Xsam-conversions=class",
            "-Xlambdas=class",
            "-Xno-source-debug-extension",
            "-jvm-target=1.8",
          ),
        associates = listOf(),
        jvmBuildTarget = jvmBuildTargetData,
      )

    val kotlincTestBuildTargetData =
      KotlinBuildTarget(
        languageVersion = "1.9",
        apiVersion = "1.9",
        kotlincOptions =
          listOf(
            "-Xno-call-assertions",
            "-Xno-param-assertions",
            "-Xsam-conversions=class",
            "-Xlambdas=class",
            "-Xno-source-debug-extension",
            "-jvm-target=1.8",
          ),
        associates = listOf(),
        jvmBuildTarget = jvmBuildTargetData,
      )

    val kotlincTestBuildTarget =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//kotlinc_test:Foo"),
        listOf("application"),
        listOf("java", "kotlin"),
        listOf(BuildTargetIdentifier(Label.synthetic("rules_kotlin_kotlin-stdlibs").toString())),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = true
          it.canDebug = false
        },
      )
    kotlincTestBuildTarget.displayName = "@//kotlinc_test:Foo"
    kotlincTestBuildTarget.baseDirectory = "file://\$WORKSPACE/kotlinc_test/"
    kotlincTestBuildTarget.data = kotlincTestBuildTargetData
    kotlincTestBuildTarget.dataKind = "kotlin"

    val openForTestingBuildTarget =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//plugin_allopen_test:open_for_testing"),
        listOf("library"),
        listOf("java", "kotlin"),
        listOf(BuildTargetIdentifier(Label.synthetic("rules_kotlin_kotlin-stdlibs").toString())),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = false
          it.canDebug = false
        },
      )
    openForTestingBuildTarget.displayName = "@//plugin_allopen_test:open_for_testing"
    openForTestingBuildTarget.baseDirectory = "file://\$WORKSPACE/plugin_allopen_test/"
    openForTestingBuildTarget.data = kotlinBuildTargetData
    openForTestingBuildTarget.dataKind = "kotlin"

    val userBuildTargetData =
      KotlinBuildTarget(
        languageVersion = "1.9",
        apiVersion = "1.9",
        kotlincOptions =
          listOf(
            "-P",
            "-Xlambdas=class",
            "-Xno-source-debug-extension",
            "-Xplugin=\$BAZEL_OUTPUT_BASE_PATH/external/com_github_jetbrains_kotlin/lib/allopen-compiler-plugin.jar",
            "-Xsam-conversions=class",
            "-jvm-target=1.8",
            "plugin:org.jetbrains.kotlin.allopen:annotation=plugin.allopen.OpenForTesting",
          ),
        associates = listOf(),
        jvmBuildTarget = jvmBuildTargetData,
      )

    val userOfExportBuildTargetData =
      KotlinBuildTarget(
        languageVersion = "1.9",
        apiVersion = "1.9",
        kotlincOptions =
          listOf(
            "-P",
            "-Xlambdas=class",
            "-Xno-source-debug-extension",
            "-Xplugin=\$BAZEL_OUTPUT_BASE_PATH/external/com_github_jetbrains_kotlin/lib/allopen-compiler-plugin.jar",
            "-Xsam-conversions=class",
            "-jvm-target=1.8",
            "plugin:org.jetbrains.kotlin.allopen:annotation=plugin.allopen.OpenForTesting",
          ),
        associates = listOf(),
        jvmBuildTarget = jvmBuildTargetData,
      )

    val userBuildTarget =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//plugin_allopen_test:user"),
        listOf("library"),
        listOf("java", "kotlin"),
        listOf(
          BuildTargetIdentifier(Label.synthetic("rules_kotlin_kotlin-stdlibs").toString()),
          BuildTargetIdentifier("@//plugin_allopen_test:open_for_testing"),
          BuildTargetIdentifier(Label.synthetic("allopen-compiler-plugin.jar").toString()),
        ),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = false
          it.canDebug = false
        },
      )
    userBuildTarget.displayName = "@//plugin_allopen_test:user"
    userBuildTarget.baseDirectory = "file://\$WORKSPACE/plugin_allopen_test/"
    userBuildTarget.data = userBuildTargetData
    userBuildTarget.dataKind = "kotlin"

    val userOfExportBuildTarget =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//plugin_allopen_test:user_of_export"),
        listOf("library"),
        listOf("java", "kotlin"),
        listOf(
          BuildTargetIdentifier(Label.synthetic("rules_kotlin_kotlin-stdlibs").toString()),
          BuildTargetIdentifier("@//plugin_allopen_test:open_for_testing_export"),
          BuildTargetIdentifier(Label.synthetic("allopen-compiler-plugin.jar").toString()),
        ),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = false
          it.canDebug = false
        },
      )
    userOfExportBuildTarget.displayName = "@//plugin_allopen_test:user_of_export"
    userOfExportBuildTarget.baseDirectory = "file://\$WORKSPACE/plugin_allopen_test/"
    userOfExportBuildTarget.data = userOfExportBuildTargetData
    userOfExportBuildTarget.dataKind = "kotlin"

    val openForTestingExport =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//plugin_allopen_test:open_for_testing_export"),
        listOf("library"),
        listOf("java", "kotlin"),
        listOf(
          BuildTargetIdentifier(Label.synthetic("rules_kotlin_kotlin-stdlibs").toString()),
          BuildTargetIdentifier("@//plugin_allopen_test:open_for_testing"),
        ),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = false
          it.canDebug = false
        },
      )
    openForTestingExport.displayName = "@//plugin_allopen_test:open_for_testing_export"
    openForTestingExport.baseDirectory = "file://\$WORKSPACE/plugin_allopen_test/"
    openForTestingExport.data = kotlinBuildTargetData
    openForTestingExport.dataKind = "kotlin"

    return WorkspaceBuildTargetsResult(
      listOf(
        kotlincTestBuildTarget,
        openForTestingBuildTarget,
        userBuildTarget,
        userOfExportBuildTarget,
        openForTestingExport,
      ),
    )
  }

  private fun compareWorkspaceTargetsResults(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "compare workspace targets results",
    ) { testClient.testWorkspaceTargets(60.seconds, expectedWorkspaceBuildTargetsResult()) }
}
