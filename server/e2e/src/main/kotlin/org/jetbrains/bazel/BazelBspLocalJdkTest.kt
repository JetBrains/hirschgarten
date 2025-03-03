package org.jetbrains.bazel

import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import kotlin.time.Duration.Companion.seconds

object BazelBspLocalJdkTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-95
  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> = listOf(workspaceBuildTargets())

  private fun workspaceBuildTargets(): BazelBspTestScenarioStep {
    val workspaceBuildTargetsResult = expectedWorkspaceBuildTargetsResult()

    return BazelBspTestScenarioStep("workspace build targets") {
      testClient.testWorkspaceTargets(
        60.seconds,
        workspaceBuildTargetsResult,
      )
    }
  }

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val javaHomePrefix =
      if (isBzlmod) {
        "rules_java${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}toolchains$bzlmodRepoNameSeparator"
      } else {
        ""
      }

    val exampleExampleJvmBuildTarget =
      JvmBuildTarget(
        javaHome = "file://\$BAZEL_OUTPUT_BASE_PATH/external/${javaHomePrefix}local_jdk/",
        javaVersion = "17",
      )

    val exampleExampleBuildTarget =
      BuildTarget(
        Label.parse("$targetPrefix//example:example"),
        listOf("application"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = true,
          canDebug = false,
        ),
        displayName = "//example",
        baseDirectory = "file://\$WORKSPACE/example/",
        exampleExampleJvmBuildTarget,
      )

    return WorkspaceBuildTargetsResult(
      listOf(exampleExampleBuildTarget),
    )
  }
}
