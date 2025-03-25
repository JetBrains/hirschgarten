package org.jetbrains.bazel

import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.commons.utils.OsArch
import org.jetbrains.bazel.commons.utils.OsFamily
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.DependencySourcesItem
import org.jetbrains.bsp.protocol.DependencySourcesParams
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.PythonOptionsItem
import org.jetbrains.bsp.protocol.PythonOptionsParams
import org.jetbrains.bsp.protocol.PythonOptionsResult
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

object BazelBspPythonProjectTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()

  private val externalRepoPrefix = if (isBzlmod) "@@" else "@"

  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      workspaceBuildTargets(),
      dependencySourcesResults(),
      pythonOptionsResults(),
    )

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val architecturePart = if (OsArch.inferFromSystem() == OsArch.ARM64) "aarch64" else "x86_64"
    val osPart = if (OsFamily.inferFromSystem() == OsFamily.MACOS) "apple-darwin" else "unknown-linux-gnu"
    val workspaceInterpreterPath = "file://\$BAZEL_OUTPUT_BASE_PATH/external/python3_9_$architecturePart-$osPart/bin/python3"
    val bzlmodInterpreterPath =
      "file://\$BAZEL_OUTPUT_BASE_PATH/external/rules_python${bzlmodRepoNameSeparator}$bzlmodRepoNameSeparator" +
        "python${bzlmodRepoNameSeparator}python_3_9_$architecturePart-$osPart/bin/python3"

    val interpreterPath = if (isBzlmod) bzlmodInterpreterPath else workspaceInterpreterPath

    val examplePythonBuildTarget =
      PythonBuildTarget(
        version = "PY3",
        interpreter = interpreterPath,
      )

    val exampleExampleBuildTarget =
      BuildTarget(
        Label.parse("$targetPrefix//example:example"),
        listOf("application"),
        listOf("python"),
        listOf(
          Label.parse("$targetPrefix//lib:example_library"),
        ),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = true,
          canDebug = false,
        ),
        displayName = "//example",
        baseDirectory = "file://\$WORKSPACE/example/",
        data = examplePythonBuildTarget,
        sources =
          listOf(
            SourceItem(
              uri = "file://\$WORKSPACE/example/example.py",
              generated = false,
            ),
          ),
        resources = listOf(),
      )

    val workspacePipDepId = "${externalRepoPrefix}pip_deps_numpy//:pkg"
    val bzlmodPipDepId =
      "@@rules_python${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}pip${bzlmodRepoNameSeparator}pip_deps_39_numpy//:pkg"
    val pipDepId = if (isBzlmod) bzlmodPipDepId else workspacePipDepId

    val exampleExampleLibBuildTarget =
      BuildTarget(
        Label.parse("$targetPrefix//lib:example_library"),
        listOf("library"),
        listOf("python"),
        listOf(Label.parse(pipDepId)),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = false,
          canDebug = false,
        ),
        displayName = "//lib:example_library",
        baseDirectory = "file://\$WORKSPACE/lib/",
        data = examplePythonBuildTarget,
        sources =
          listOf(
            SourceItem(
              uri = "file://\$WORKSPACE/lib/example_lib.py",
              generated = false,
            ),
          ),
        resources = listOf(),
      )

    val exampleExampleTestBuildTarget =
      BuildTarget(
        Label.parse("$targetPrefix//test:test"),
        listOf("test"),
        listOf("python"),
        listOf(),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = true,
          canRun = false,
          canDebug = false,
        ),
        displayName = "//test",
        baseDirectory = "file://\$WORKSPACE/test/",
        data = examplePythonBuildTarget,
        sources =
          listOf(
            SourceItem(
              uri = "file://\$WORKSPACE/test/test.py",
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
      testClient.testWorkspaceTargets(
        1.minutes,
        workspaceBuildTargetsResult,
      )
    }
  }

  private fun dependencySourcesResults(): BazelBspTestScenarioStep {
    val workspacePipPath = "file://\$BAZEL_OUTPUT_BASE_PATH/external/pip_deps_numpy/site-packages/"
    val bzlmodPipPath =
      "file://\$BAZEL_OUTPUT_BASE_PATH/external/rules_python${bzlmodRepoNameSeparator}$bzlmodRepoNameSeparator" +
        "pip${bzlmodRepoNameSeparator}pip_deps_39_numpy/site-packages/"
    val pipPath = if (isBzlmod) bzlmodPipPath else workspacePipPath

    val expectedPythonDependencySourcesItems =
      expectedWorkspaceBuildTargetsResult().targets.map {
        if (it.id == Label.parse("$targetPrefix//lib:example_library")) {
          DependencySourcesItem(
            it.id,
            listOf(pipPath),
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
      testClient.testDependencySources(
        30.seconds,
        dependencySourcesParams,
        expectedDependencies,
      )
    }
  }

  private fun pythonOptionsResults(): BazelBspTestScenarioStep {
    val expectedTargetIdentifiers = expectedTargetIdentifiers().filter { it != Label.synthetic("bsp-workspace-root") }
    val expectedPythonOptionsItems = expectedTargetIdentifiers.map { PythonOptionsItem(it, emptyList()) }
    val expectedPythonOptionsResult = PythonOptionsResult(expectedPythonOptionsItems)
    val pythonOptionsParams = PythonOptionsParams(expectedTargetIdentifiers)

    return BazelBspTestScenarioStep(
      "pythonOptions results",
    ) {
      testClient.testPythonOptions(30.seconds, pythonOptionsParams, expectedPythonOptionsResult)
    }
  }
}
