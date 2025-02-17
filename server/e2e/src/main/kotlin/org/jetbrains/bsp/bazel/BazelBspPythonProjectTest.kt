package org.jetbrains.bsp.bazel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetDataKind
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.DependencySourcesItem
import ch.epfl.scala.bsp4j.DependencySourcesParams
import ch.epfl.scala.bsp4j.DependencySourcesResult
import ch.epfl.scala.bsp4j.PythonBuildTarget
import ch.epfl.scala.bsp4j.PythonOptionsItem
import ch.epfl.scala.bsp4j.PythonOptionsParams
import ch.epfl.scala.bsp4j.PythonOptionsResult
import ch.epfl.scala.bsp4j.ResourcesItem
import ch.epfl.scala.bsp4j.ResourcesParams
import ch.epfl.scala.bsp4j.ResourcesResult
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import org.jetbrains.bazel.commons.utils.OsArch
import org.jetbrains.bazel.commons.utils.OsFamily
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
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
      PythonBuildTarget().also {
        it.version = "PY3"
        it.interpreter = interpreterPath
      }

    val exampleExampleBuildTarget =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//example:example"),
        listOf("application"),
        listOf("python"),
        listOf(
          BuildTargetIdentifier("$targetPrefix//lib:example_library"),
        ),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = true
          it.canDebug = false
        },
      )
    exampleExampleBuildTarget.displayName = "$targetPrefix//example:example"
    exampleExampleBuildTarget.baseDirectory = "file://\$WORKSPACE/example/"
    exampleExampleBuildTarget.data = examplePythonBuildTarget
    exampleExampleBuildTarget.dataKind = BuildTargetDataKind.PYTHON

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
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = false
          it.canDebug = false
        },
      )
    exampleExampleLibBuildTarget.displayName = "$targetPrefix//lib:example_library"
    exampleExampleLibBuildTarget.baseDirectory = "file://\$WORKSPACE/lib/"
    exampleExampleLibBuildTarget.data = examplePythonBuildTarget
    exampleExampleLibBuildTarget.dataKind = BuildTargetDataKind.PYTHON

    val exampleExampleTestBuildTarget =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//test:test"),
        listOf("test"),
        listOf("python"),
        listOf(),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = true
          it.canRun = false
          it.canDebug = false
        },
      )
    exampleExampleTestBuildTarget.displayName = "$targetPrefix//test:test"
    exampleExampleTestBuildTarget.baseDirectory = "file://\$WORKSPACE/test/"
    exampleExampleTestBuildTarget.data = examplePythonBuildTarget
    exampleExampleTestBuildTarget.dataKind = BuildTargetDataKind.PYTHON

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
