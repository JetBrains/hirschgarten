package org.jetbrains.bsp.bazel

import org.jetbrains.bazel.commons.utils.OsArch
import org.jetbrains.bazel.commons.utils.OsFamily
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.DependencySourcesItem
import org.jetbrains.bsp.protocol.DependencySourcesParams
import org.jetbrains.bsp.protocol.DependencySourcesResult
import org.jetbrains.bsp.protocol.PythonBuildTarget
import org.jetbrains.bsp.protocol.PythonOptionsItem
import org.jetbrains.bsp.protocol.PythonOptionsParams
import org.jetbrains.bsp.protocol.PythonOptionsResult
import org.jetbrains.bsp.protocol.ResourcesItem
import org.jetbrains.bsp.protocol.ResourcesParams
import org.jetbrains.bsp.protocol.ResourcesResult
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
      resourcesResults(),
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
        BuildTargetIdentifier("$targetPrefix//example:example"),
        listOf("application"),
        listOf("python"),
        listOf(
          BuildTargetIdentifier("$targetPrefix//lib:example_library"),
        ),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = true,
          canDebug = false,
        ),
        displayName = "$targetPrefix//example:example",
        baseDirectory = "file://\$WORKSPACE/example/",
        data = examplePythonBuildTarget,
      )

    val workspacePipDepId = "${externalRepoPrefix}pip_deps_numpy//:pkg"
    val bzlmodPipDepId =
      "@@rules_python${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}pip${bzlmodRepoNameSeparator}pip_deps_39_numpy//:pkg"
    val pipDepId = if (isBzlmod) bzlmodPipDepId else workspacePipDepId

    val exampleExampleLibBuildTarget =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//lib:example_library"),
        listOf("library"),
        listOf("python"),
        listOf(BuildTargetIdentifier(pipDepId)),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = false,
          canRun = false,
          canDebug = false,
        ),
        displayName = "$targetPrefix//lib:example_library",
        baseDirectory = "file://\$WORKSPACE/lib/",
        data = examplePythonBuildTarget,
      )

    val exampleExampleTestBuildTarget =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//test:test"),
        listOf("test"),
        listOf("python"),
        listOf(),
        BuildTargetCapabilities(
          canCompile = true,
          canTest = true,
          canRun = false,
          canDebug = false,
        ),
        displayName = "$targetPrefix//test:test",
        baseDirectory = "file://\$WORKSPACE/test/",
        data = examplePythonBuildTarget,
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
        if (it.id == BuildTargetIdentifier("$targetPrefix//lib:example_library")) {
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
    val expectedTargetIdentifiers = expectedTargetIdentifiers().filter { it.uri != "bsp-workspace-root" }
    val expectedPythonOptionsItems = expectedTargetIdentifiers.map { PythonOptionsItem(it, emptyList()) }
    val expectedPythonOptionsResult = PythonOptionsResult(expectedPythonOptionsItems)
    val pythonOptionsParams = PythonOptionsParams(expectedTargetIdentifiers)

    return BazelBspTestScenarioStep(
      "pythonOptions results",
    ) {
      testClient.testPythonOptions(30.seconds, pythonOptionsParams, expectedPythonOptionsResult)
    }
  }

  private fun resourcesResults(): BazelBspTestScenarioStep {
    val expectedTargetIdentifiers = expectedTargetIdentifiers().filter { it.uri != "bsp-workspace-root" }
    val expectedResourcesItems = expectedTargetIdentifiers.map { ResourcesItem(it, emptyList()) }
    val expectedResourcesResult = ResourcesResult(expectedResourcesItems)
    val resourcesParams = ResourcesParams(expectedTargetIdentifiers)

    return BazelBspTestScenarioStep(
      "resources results",
    ) {
      testClient.testResources(30.seconds, resourcesParams, expectedResourcesResult)
    }
  }
}
