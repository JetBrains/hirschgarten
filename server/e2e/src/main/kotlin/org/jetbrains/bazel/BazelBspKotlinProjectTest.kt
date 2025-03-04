package org.jetbrains.bazel

import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.install.Install
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
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
      JvmBuildTarget(
        javaHome = javaHome,
        javaVersion = "11",
      )

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
        Label.parse("$targetPrefix//kotlinc_test:Foo"),
        tags = listOf("application"),
        languageIds = listOf("java", "kotlin"),
        dependencies = listOf(Label.parse(Label.synthetic("rules_kotlin_kotlin-stdlibs").toString())),
        capabilities =
          BuildTargetCapabilities(
            canCompile = true,
            canTest = false,
            canRun = true,
            canDebug = false,
          ),
        displayName = "//kotlinc_test:Foo",
        baseDirectory = "file://\$WORKSPACE/kotlinc_test/",
        data = kotlincTestBuildTargetData,
      )

    val openForTestingBuildTarget =
      BuildTarget(
        Label.parse("$targetPrefix//plugin_allopen_test:open_for_testing"),
        tags = listOf("library"),
        languageIds = listOf("java", "kotlin"),
        dependencies = listOf(Label.parse(Label.synthetic("rules_kotlin_kotlin-stdlibs").toString())),
        capabilities =
          BuildTargetCapabilities(
            canCompile = true,
            canTest = false,
            canRun = false,
            canDebug = false,
          ),
        displayName = "//plugin_allopen_test:open_for_testing",
        baseDirectory = "file://\$WORKSPACE/plugin_allopen_test/",
        data = kotlinBuildTargetData,
      )

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
        Label.parse("$targetPrefix//plugin_allopen_test:user"),
        tags = listOf("library"),
        languageIds = listOf("java", "kotlin"),
        dependencies =
          listOf(
            Label.parse(Label.synthetic("rules_kotlin_kotlin-stdlibs").toString()),
            Label.parse("@//plugin_allopen_test:open_for_testing"),
            Label.parse(Label.synthetic("allopen-compiler-plugin.jar").toString()),
          ),
        capabilities =
          BuildTargetCapabilities(
            canCompile = true,
            canTest = false,
            canRun = false,
            canDebug = false,
          ),
        displayName = "//plugin_allopen_test:user",
        baseDirectory = "file://\$WORKSPACE/plugin_allopen_test/",
        data = userBuildTargetData,
      )

    val userOfExportBuildTarget =
      BuildTarget(
        Label.parse("$targetPrefix//plugin_allopen_test:user_of_export"),
        tags = listOf("library"),
        languageIds = listOf("java", "kotlin"),
        dependencies =
          listOf(
            Label.parse(Label.synthetic("rules_kotlin_kotlin-stdlibs").toString()),
            Label.parse("@//plugin_allopen_test:open_for_testing_export"),
            Label.parse(Label.synthetic("allopen-compiler-plugin.jar").toString()),
          ),
        capabilities =
          BuildTargetCapabilities(
            canCompile = true,
            canTest = false,
            canRun = false,
            canDebug = false,
          ),
        displayName = "//plugin_allopen_test:user_of_export",
        baseDirectory = "file://\$WORKSPACE/plugin_allopen_test/",
        data = userOfExportBuildTargetData,
      )

    val openForTestingExport =
      BuildTarget(
        Label.parse("$targetPrefix//plugin_allopen_test:open_for_testing_export"),
        tags = listOf("library"),
        languageIds = listOf("java", "kotlin"),
        dependencies =
          listOf(
            Label.parse(Label.synthetic("rules_kotlin_kotlin-stdlibs").toString()),
            Label.parse("@//plugin_allopen_test:open_for_testing"),
          ),
        capabilities =
          BuildTargetCapabilities(
            canCompile = true,
            canTest = false,
            canRun = false,
            canDebug = false,
          ),
        displayName = "//plugin_allopen_test:open_for_testing_export",
        baseDirectory = "file://\$WORKSPACE/plugin_allopen_test/",
        data = kotlinBuildTargetData,
      )

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
