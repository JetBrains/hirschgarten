package org.jetbrains.bazel

import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

object ExternalAutoloadsTest : BazelBspTestBaseScenario() {
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
    val javaHome =
      Path(
        "\$BAZEL_OUTPUT_BASE_PATH/external/rules_java${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}toolchains${bzlmodRepoNameSeparator}remotejdk11_$javaHomeArchitecture/",
      )

    val exampleExampleJvmBuildTarget =
      JvmBuildTarget(
        javaVersion = "11",
        javaHome = javaHome,
      )

    val exampleExampleBuildTarget =
      BuildTarget(
        Label.parse("$targetPrefix//src:hello"),
        listOf("library"),
        listOf("java"),
        emptyList(),
        TargetKind(
          kindString = "java_library",
          ruleType = RuleType.LIBRARY,
        ),
        baseDirectory = Path("\$WORKSPACE/src/"),
        data = exampleExampleJvmBuildTarget,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/src/Hello.java"),
              generated = false,
              jvmPackagePrefix = "src",
            ),
          ),
        resources = emptyList(),
      )

    return WorkspaceBuildTargetsResult(
      listOf(exampleExampleBuildTarget),
    )
  }
}
