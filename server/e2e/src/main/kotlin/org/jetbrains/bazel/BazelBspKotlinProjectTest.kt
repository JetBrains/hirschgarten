package org.jetbrains.bazel

import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.install.Install
import org.jetbrains.bazel.install.cli.CliOptions
import org.jetbrains.bazel.install.cli.ProjectViewCliOptions
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.JavacOptionsItem
import org.jetbrains.bsp.protocol.JavacOptionsParams
import org.jetbrains.bsp.protocol.JavacOptionsResult
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.KotlinBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.jetbrains.bsp.protocol.WorkspaceNameResult
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds

open class BazelBspKotlinProjectTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()

  override fun installServer() {
    Install.runInstall(
      CliOptions(
        workspaceDir = Path(workspaceDir),
        projectViewCliOptions =
          ProjectViewCliOptions(
            bazelBinary = Path(bazelBinary),
            targets = listOf("//..."),
            directories = listOf(workspaceDir),
            shardSync = true,
            targetShardSize = 1,
          ),
      ),
    )
  }

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      compareWorkspaceTargetsResults(),
      compareBazelWorkspaceNameResults(),
    )

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val workspaceJavaHome = Path("\$BAZEL_OUTPUT_BASE_PATH/external/remotejdk11_$javaHomeArchitecture/")
    val bzlmodJavaHome =
      Path(
        "\$BAZEL_OUTPUT_BASE_PATH/external/rules_java$bzlmodRepoNameSeparator" +
          "${bzlmodRepoNameSeparator}toolchains${bzlmodRepoNameSeparator}remotejdk11_$javaHomeArchitecture/",
      )
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
      RawBuildTarget(
        Label.parse("$targetPrefix//kotlinc_test:Foo"),
        tags = listOf(),
        dependencies = listOf(Label.synthetic("rules_kotlin_kotlin-stdlibs")),
        kind =
          TargetKind(
            kindString = "kt_jvm_binary",
            ruleType = RuleType.BINARY,
            languageClasses = setOf(LanguageClass.KOTLIN, LanguageClass.JAVA),
          ),
        baseDirectory = Path("\$WORKSPACE/kotlinc_test/"),
        data = kotlincTestBuildTargetData,
        sources =
          listOf(
            SourceItem(
              generated = false,
              path = Path("\$WORKSPACE/kotlinc_test/Foo.kt"),
            ),
          ),
        resources = emptyList(),
      )

    val openForTestingBuildTarget =
      RawBuildTarget(
        Label.parse("$targetPrefix//plugin_allopen_test:open_for_testing"),
        tags = listOf(),
        dependencies = listOf(Label.synthetic("rules_kotlin_kotlin-stdlibs")),
        kind =
          TargetKind(
            kindString = "kt_jvm_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.KOTLIN, LanguageClass.JAVA),
          ),
        baseDirectory = Path("\$WORKSPACE/plugin_allopen_test/"),
        data = kotlinBuildTargetData,
        sources =
          listOf(
            SourceItem(
              generated = false,
              path = Path("\$WORKSPACE/plugin_allopen_test/OpenForTesting.kt"),
              jvmPackagePrefix = "plugin.allopen",
            ),
          ),
        resources = emptyList(),
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
      RawBuildTarget(
        Label.parse("$targetPrefix//plugin_allopen_test:user"),
        tags = listOf(),
        dependencies =
          listOf(
            Label.synthetic("rules_kotlin_kotlin-stdlibs"),
            Label.parse("@//plugin_allopen_test:open_for_testing"),
            Label.synthetic("allopen-compiler-plugin.jar"),
          ),
        kind =
          TargetKind(
            kindString = "kt_jvm_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.KOTLIN, LanguageClass.JAVA),
          ),
        baseDirectory = Path("\$WORKSPACE/plugin_allopen_test/"),
        data = userBuildTargetData,
        sources =
          listOf(
            SourceItem(
              generated = false,
              path = Path("\$WORKSPACE/plugin_allopen_test/User.kt"),
              jvmPackagePrefix = "plugin.allopen",
            ),
          ),
        resources = emptyList(),
      )

    val userOfExportBuildTarget =
      RawBuildTarget(
        Label.parse("$targetPrefix//plugin_allopen_test:user_of_export"),
        tags = listOf(),
        dependencies =
          listOf(
            Label.synthetic("rules_kotlin_kotlin-stdlibs"),
            Label.parse("@//plugin_allopen_test:open_for_testing_export"),
            Label.synthetic("allopen-compiler-plugin.jar"),
          ),
        kind =
          TargetKind(
            kindString = "kt_jvm_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.KOTLIN, LanguageClass.JAVA),
          ),
        baseDirectory = Path("\$WORKSPACE/plugin_allopen_test/"),
        data = userOfExportBuildTargetData,
        sources =
          listOf(
            SourceItem(
              generated = false,
              path = Path("\$WORKSPACE/plugin_allopen_test/User.kt"),
              jvmPackagePrefix = "plugin.allopen",
            ),
          ),
        resources = emptyList(),
      )

    val openForTestingExport =
      RawBuildTarget(
        Label.parse("$targetPrefix//plugin_allopen_test:open_for_testing_export"),
        tags = listOf(),
        dependencies =
          listOf(
            Label.synthetic("rules_kotlin_kotlin-stdlibs"),
            Label.parse("@//plugin_allopen_test:open_for_testing"),
          ),
        kind =
          TargetKind(
            kindString = "kt_jvm_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.KOTLIN, LanguageClass.JAVA),
          ),
        baseDirectory = Path("\$WORKSPACE/plugin_allopen_test/"),
        data = kotlinBuildTargetData,
        sources =
          listOf(),
        resources = emptyList(),
      )

    return WorkspaceBuildTargetsResult(
      targets = mapOf(),
      rootTargets = setOf(),
    )
  }

  private fun compareWorkspaceTargetsResults(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "compare workspace targets results",
    ) { testClient.testWorkspaceTargets(140.seconds, expectedWorkspaceBuildTargetsResult()) }

  private fun compareBazelWorkspaceNameResults(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "compare workspace targets results",
    ) {
      val workspaceName =
        if (isBzlmod) {
          "_main"
        } else {
          "kt_prj"
        }
      testClient.testWorkspaceName(140.seconds, WorkspaceNameResult(workspaceName = workspaceName))
    }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) = BazelBspKotlinProjectTest().executeScenario()
  }
}
