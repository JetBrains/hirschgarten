package org.jetbrains.bazel

import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import kotlin.io.path.Path
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
    val javaHomeBazel5And6 = Path("\$BAZEL_OUTPUT_BASE_PATH/external/remotejdk11_$javaHomeArchitecture/")
    val javaHomeBazel7 =
      Path(
        "\$BAZEL_OUTPUT_BASE_PATH/external/rules_java${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}toolchains${bzlmodRepoNameSeparator}remotejdk11_$javaHomeArchitecture/",
      )
    val javaHome = if (isBzlmod) javaHomeBazel7 else javaHomeBazel5And6

    val exampleExampleJvmBuildTarget =
      JvmBuildTarget(
        javaVersion = "11",
        javaHome = javaHome,
      )

    val exampleExampleBuildTarget =
      RawBuildTarget(
        Label.parse("$targetPrefix//example:example"),
        listOf(),
        emptyList(),
        TargetKind(
          kindString = "java_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.JAVA),
        ),
        baseDirectory = Path("\$WORKSPACE/example/"),
        data = listOf(exampleExampleJvmBuildTarget),
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/example/Example.java"),
              generated = false,
              jvmPackagePrefix = "example",
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
