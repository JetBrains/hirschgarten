package org.jetbrains.bazel

import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import kotlin.time.Duration.Companion.minutes

object BazelBspRemoteJdkTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-95
  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> = listOf(workspaceBuildTargets())

  private fun workspaceBuildTargets(): BazelBspTestScenarioStep {
    val workspaceBuildTargetsResult = expectedWorkspaceBuildTargetsResult()

    return BazelBspTestScenarioStep("workspace build targets") {
      testClient.testWorkspaceTargets(
        1.minutes,
        workspaceBuildTargetsResult,
      )
    }
  }

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val javaHomeBazel5And6 = "file://\$BAZEL_OUTPUT_BASE_PATH/external/remotejdk11_$javaHomeArchitecture/"
    val javaHomeBazel7 =
      "file://\$BAZEL_OUTPUT_BASE_PATH/external/" +
        "rules_java${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}toolchains${bzlmodRepoNameSeparator}remotejdk11_$javaHomeArchitecture/"
    val javaHome = if (isBzlmod) javaHomeBazel7 else javaHomeBazel5And6

    val exampleExampleJvmBuildTarget =
      JvmBuildTarget(
        javaVersion = "11",
        javaHome = javaHome,
      )

    val exampleExampleBuildTarget =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//example:example"),
        listOf("application"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = true,
          canDebug = false,
        ),
        displayName = "$targetPrefix//example:example",
        baseDirectory = "file://\$WORKSPACE/example/",
        data = exampleExampleJvmBuildTarget,
      )

    return WorkspaceBuildTargetsResult(
      listOf(exampleExampleBuildTarget),
    )
  }
}
