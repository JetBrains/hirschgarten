package org.jetbrains.bazel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.JvmBuildTarget
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.install.Install
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import kotlin.time.Duration.Companion.seconds

open class BazelBspKotlinProjectTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()

  override fun installServer() {
    Install.main(
      arrayOf(
        "-d",
        workspaceDir,
        "-b",
        bazelBinary,
        "-t",
        "//...",
        "--shard-sync",
        "true",
        "--target-shard-size",
        "1",
      ),
    )
  }

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      compareWorkspaceTargetsResults(),
    )

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val workspaceJavaHome = "file://\$BAZEL_OUTPUT_BASE_PATH/external/remotejdk11_$javaHomeArchitecture/"
    val bzlmodJavaHome =
      "file://\$BAZEL_OUTPUT_BASE_PATH/external/rules_java$bzlmodRepoNameSeparator" +
        "${bzlmodRepoNameSeparator}toolchains${bzlmodRepoNameSeparator}remotejdk11_$javaHomeArchitecture/"
    val javaHome = if (isBzlmod) bzlmodJavaHome else workspaceJavaHome
    val jvmBuildTargetData =
      JvmBuildTarget().also {
        it.javaHome = javaHome
        it.javaVersion = "11"
      }

    val kotlinBuildTargetData =
      KotlinBuildTarget(
        languageVersion = "1.9",
        apiVersion = "1.9",
        kotlincOptions =
          listOfNotNull(
            "-Xsam-conversions=class",
            "-Xlambdas=class",
            if (isBzlmod) null else "-Xno-source-debug-extension",
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
          listOfNotNull(
            "-Xno-call-assertions",
            "-Xno-param-assertions",
            "-Xsam-conversions=class",
            "-Xlambdas=class",
            if (isBzlmod) null else "-Xno-source-debug-extension",
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

    val bzlmodPluginRepo =
      "rules_kotlin${bzlmodRepoNameSeparator}$bzlmodRepoNameSeparator" +
        "rules_kotlin_extensions${bzlmodRepoNameSeparator}com_github_jetbrains_kotlin_git"
    val workspacePluginRepo = "com_github_jetbrains_kotlin"
    val pluginRepo = if (isBzlmod) bzlmodPluginRepo else workspacePluginRepo

    val userBuildTargetData =
      KotlinBuildTarget(
        languageVersion = "1.9",
        apiVersion = "1.9",
        kotlincOptions =
          listOfNotNull(
            "-P",
            "-Xlambdas=class",
            if (isBzlmod) null else "-Xno-source-debug-extension",
            "-Xplugin=\$BAZEL_OUTPUT_BASE_PATH/external/$pluginRepo/lib/allopen-compiler-plugin.jar",
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
          listOfNotNull(
            "-P",
            "-Xlambdas=class",
            if (isBzlmod) null else "-Xno-source-debug-extension",
            "-Xplugin=\$BAZEL_OUTPUT_BASE_PATH/external/$pluginRepo/lib/allopen-compiler-plugin.jar",
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
    ) { testClient.testWorkspaceTargets(140.seconds, expectedWorkspaceBuildTargetsResult()) }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) = BazelBspKotlinProjectTest().executeScenario()
  }
}
