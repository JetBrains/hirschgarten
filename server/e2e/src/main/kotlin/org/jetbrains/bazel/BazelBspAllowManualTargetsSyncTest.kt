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
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.seconds

object BazelBspAllowManualTargetsSyncTest : BazelBspTestBaseScenario() {
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
            targets = listOf("//..."),
            allowManualTargetsSync = true,
          ),
      ),
    )
  }

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      compareWorkspaceTargetsResults(),
    )

  private fun compareWorkspaceTargetsResults(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "compare workspace targets results",
    ) { testClient.testWorkspaceTargets(30.seconds, expectedWorkspaceBuildTargetsResult()) }

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val architecturePart = if (System.getProperty("os.arch") == "aarch64") "_aarch64" else ""
    val javaHomeBazel5And6 = Path("\$BAZEL_OUTPUT_BASE_PATH/external/remotejdk11_\$OS$architecturePart/")
    val javaHomeBazel7 =
      Path(
        "\$BAZEL_OUTPUT_BASE_PATH/external/rules_java${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}toolchains${bzlmodRepoNameSeparator}remotejdk11_\$OS$architecturePart/",
      )
    val javaHome = if (isBzlmod) javaHomeBazel7 else javaHomeBazel5And6
    val jvmBuildTarget =
      JvmBuildTarget(
        javaHome = javaHome,
        javaVersion = "11",
      )

    val manualTargetJavaLibrary =
      RawBuildTarget(
        Label.parse("$targetPrefix//manual_target:java_library"),
        tags = listOf("manual"),
        dependencies = emptyList(),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
        baseDirectory = Path("\$WORKSPACE/manual_target/"),
        data = listOf(jvmBuildTarget),
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/manual_target/TestJavaFile.java"),
              generated = false,
              jvmPackagePrefix = "manual_target",
            ),
          ),
        resources = emptyList(),
      )

    val manualTargetJavaBinary =
      RawBuildTarget(
        Label.parse("$targetPrefix//manual_target:java_binary"),
        tags = listOf("manual"),
        dependencies = emptyList(),
        kind =
          TargetKind(
            kindString = "java_binary",
            ruleType = RuleType.BINARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
        baseDirectory = Path("\$WORKSPACE/manual_target/"),
        data = listOf(jvmBuildTarget),
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/manual_target/TestJavaBinary.java"),
              generated = false,
              jvmPackagePrefix = "manual_target",
            ),
          ),
        resources = emptyList(),
      )

    val manualTargetJavaTest =
      RawBuildTarget(
        Label.parse("$targetPrefix//manual_target:java_test"),
        tags = listOf("manual"),
        dependencies = emptyList(),
        kind =
          TargetKind(
            kindString = "java_test",
            ruleType = RuleType.TEST,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
        baseDirectory = Path("\$WORKSPACE/manual_target/"),
        data = listOf(jvmBuildTarget),
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/manual_target/JavaTest.java"),
              generated = false,
              jvmPackagePrefix = "manual_target",
            ),
          ),
        resources = emptyList(),
      )

    return WorkspaceBuildTargetsResult(
      targets = mapOf(),
      rootTargets = setOf(),
    )
  }
}
