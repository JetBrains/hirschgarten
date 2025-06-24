package org.jetbrains.bazel

import com.intellij.openapi.util.SystemInfo
import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.DependencySourcesItem
import org.jetbrains.bsp.protocol.DependencySourcesParams
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.RawBuildTarget
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object BazelBspPythonProjectTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()

  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      workspaceBuildTargets(),
      dependencySourcesResults(),
    )

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val architecturePart = if (SystemInfo.isAarch64) "aarch64" else "x86_64"
    val osPart = if (SystemInfo.isMac) "apple-darwin" else "unknown-linux-gnu"
    val workspaceInterpreterPath = Path("\$BAZEL_OUTPUT_BASE_PATH/external/python3_9_$architecturePart-$osPart/bin/python3")
    val bzlmodInterpreterPath =
      Path(
        "\$BAZEL_OUTPUT_BASE_PATH/external/rules_python${bzlmodRepoNameSeparator}$bzlmodRepoNameSeparator" +
          "python${bzlmodRepoNameSeparator}python_3_9_$architecturePart-$osPart/bin/python3",
      )

    val interpreterPath = if (isBzlmod) bzlmodInterpreterPath else workspaceInterpreterPath

    val examplePythonBuildTarget =
      PythonBuildTarget(
        version = "PY3",
        interpreter = interpreterPath,
        listOf(),
        false,
        listOf(),
      )

    val exampleExampleBuildTarget =
      RawBuildTarget(
        Label.parse("$targetPrefix//example:example"),
        listOf(),
        listOf(
          Label.parse("$targetPrefix//lib:example_library"),
        ),
        TargetKind(
          kindString = "py_binary",
          ruleType = RuleType.BINARY,
          languageClasses = setOf(LanguageClass.PYTHON),
        ),
        baseDirectory = Path("\$WORKSPACE/example/"),
        data = examplePythonBuildTarget,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/example/example.py"),
              generated = false,
            ),
          ),
        resources = listOf(),
      )

    val exampleExampleLibBuildTarget =
      RawBuildTarget(
        Label.parse("$targetPrefix//lib:example_library"),
        listOf(),
        listOf(),
        TargetKind(
          kindString = "py_library",
          ruleType = RuleType.LIBRARY,
          languageClasses = setOf(LanguageClass.PYTHON),
        ),
        baseDirectory = Path("\$WORKSPACE/lib/"),
        data = examplePythonBuildTarget,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/lib/example_lib.py"),
              generated = false,
            ),
          ),
        resources = listOf(),
      )

    val exampleExampleTestBuildTarget =
      RawBuildTarget(
        Label.parse("$targetPrefix//test:test"),
        listOf(),
        listOf(),
        TargetKind(
          kindString = "py_test",
          ruleType = RuleType.TEST,
          languageClasses = setOf(LanguageClass.PYTHON),
        ),
        baseDirectory = Path("\$WORKSPACE/test/"),
        data = examplePythonBuildTarget,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/test/test.py"),
              generated = false,
            ),
          ),
        resources = listOf(),
      )

    return WorkspaceBuildTargetsResult(
      listOf(
        exampleExampleBuildTarget,
        exampleExampleLibBuildTarget,
        exampleExampleTestBuildTarget,
      ),
    )
  }

  private fun workspaceBuildTargets(): BazelBspTestScenarioStep {
    val workspaceBuildTargetsResult = expectedWorkspaceBuildTargetsResult()

    return BazelBspTestScenarioStep("workspace build targets") {
      if (isBzlmod) {
        testClient.testMainWorkspaceTargets(
          1.minutes,
          workspaceBuildTargetsResult,
        )
      }
    }
  }

  private fun dependencySourcesResults(): BazelBspTestScenarioStep {
    val expectedPythonDependencySourcesItems =
      expectedWorkspaceBuildTargetsResult().targets.map {
        if (it.id == Label.parse("$targetPrefix//lib:example_library")) {
          DependencySourcesItem(
            it.id,
            listOf(),
          )
        } else {
          DependencySourcesItem(it.id, emptyList())
        }
      }

    val expectedTargetIdentifiers = expectedTargetIdentifiers()
    val expectedDependencies = DependencySourcesResult(expectedPythonDependencySourcesItems)
    val dependencySourcesParams = DependencySourcesParams(expectedTargetIdentifiers)

    return BazelBspTestScenarioStep(
      "dependency sources results",
    ) {
      if (isBzlmod) {
        testClient.testDependencySources(
          30.seconds,
          dependencySourcesParams,
          expectedDependencies,
        )
      }
    }
  }
}
