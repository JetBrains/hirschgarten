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
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsParams
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
            enabledRules = listOf("io_bazel_rules_scala", "rules_java", "rules_jvm"),
          ),
      ),
    )
  }

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> = listOf(runInitialSyncAndPartialSync())

  private fun runInitialSyncAndPartialSync(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep("should do an initial sync on 1 target and then partial sync on another target") {
      testClient.test(3.minutes) { session ->
        // initial sync
        val workspaceBuildTargetsResult = session.server.workspaceBuildTargets(WorkspaceBuildTargetsParams("originId"))
        testClient.assertJsonEquals(expectedWorkspaceBuildTargetsResult(), workspaceBuildTargetsResult)

        // partial sync
        val partialSyncTargetId = Label.parse("$targetPrefix//java_targets:java_binary")
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
        val javaTargetsJavaBinary =
          BuildTarget(
            partialSyncTargetId,
            listOf(),
            emptyList(),
            TargetKind(
              kindString = "java_binary",
              languageClasses = setOf(LanguageClass.JAVA),
              ruleType = RuleType.BINARY,
            ),
            baseDirectory = Path("\$WORKSPACE/java_targets/"),
            data = jvmBuildTarget,
            sources = listOf(SourceItem(Path("\$WORKSPACE/java_targets/JavaBinary.java"), false, "java_targets")),
            resources = emptyList(),
          )

        val workspaceBuildTargetsPartialParams =
          WorkspaceBuildTargetsPartialParams(listOf(Label.parse("$targetPrefix//java_targets:java_binary")))
        val expectedTargetsResult = WorkspaceBuildTargetsResult(listOf(javaTargetsJavaBinary))

        val workspaceBuildTargetsPartialResult = session.server.workspaceBuildTargetsPartial(workspaceBuildTargetsPartialParams)
        testClient.assertJsonEquals(expectedTargetsResult, workspaceBuildTargetsPartialResult)
      }
    }

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

    val javaTargetsJavaLibrary =
      BuildTarget(
        Label.parse("$targetPrefix//java_targets:java_library"),
        listOf(),
        listOf(),
        TargetKind(
          kindString = "java_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirectory = Path("\$WORKSPACE/java_targets/"),
        data = jvmBuildTarget,
        sources = listOf(SourceItem(Path("\$WORKSPACE/java_targets/JavaLibrary.java"), false, "java_targets")),
        resources = emptyList(),
      )

    return WorkspaceBuildTargetsResult(listOf(javaTargetsJavaLibrary))
  }
}
