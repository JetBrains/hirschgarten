package org.jetbrains.bazel

import com.google.common.collect.ImmutableList
import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.CppBuildTarget
import org.jetbrains.bsp.protocol.CppOptionsItem
import org.jetbrains.bsp.protocol.CppOptionsParams
import org.jetbrains.bsp.protocol.CppOptionsResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import kotlin.time.Duration.Companion.seconds

object BazelBspCppProjectTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()

  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> = listOf(compareWorkspaceTargetsResults(), cppOptions())

  private fun compareWorkspaceTargetsResults(): BazelBspTestScenarioStep {
    val expectedWorkspaceBuildTargetsResult = expectedWorkspaceBuildTargetsResult()

    return BazelBspTestScenarioStep("cpp project") {
      testClient.testWorkspaceTargets(20.seconds, expectedWorkspaceBuildTargetsResult)
    }
  }

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val exampleExampleCppBuildTarget =
      CppBuildTarget(
        version = null,
        compiler = "compiler",
        cCompiler = "/bin/gcc",
        cppCompiler = "/bin/gcc",
      )

    val exampleExampleBuildTarget =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//example:example"),
        tags = ImmutableList.of("application"),
        languageIds = ImmutableList.of(Constants.CPP),
        dependencies = ImmutableList.of(BuildTargetIdentifier("@com_google_googletest//:gtest_main")),
        capabilities =
          BuildTargetCapabilities(
            canCompile = true,
            canTest = false,
            canRun = true,
            canDebug = false,
          ),
        displayName = "$targetPrefix//example:example",
        baseDirectory = "file://\$WORKSPACE/example/",
        data = exampleExampleCppBuildTarget,
      )

    val bspWorkspaceRootExampleBuildTarget =
      BuildTarget(
        BuildTargetIdentifier(Label.synthetic("bsp-workspace-root").toString()),
        tags = ImmutableList.of(),
        languageIds = ImmutableList.of(),
        dependencies = ImmutableList.of(),
        capabilities =
          BuildTargetCapabilities(
            canCompile = false,
            canTest = false,
            canRun = false,
            canDebug = false,
          ),
        baseDirectory = "file://\$WORKSPACE/",
        displayName = "bsp-workspace-root",
      )
    return WorkspaceBuildTargetsResult(ImmutableList.of(exampleExampleBuildTarget, bspWorkspaceRootExampleBuildTarget))
  }

  private fun cppOptions(): BazelBspTestScenarioStep {
    val cppOptionsParams = CppOptionsParams(ImmutableList.of(BuildTargetIdentifier("$targetPrefix//example:example")))

    val exampleExampleCppOptionsItem =
      CppOptionsItem(
        BuildTargetIdentifier("$targetPrefix//example:example"),
        ImmutableList.of("-Iexternal/gtest/include"),
        ImmutableList.of("BOOST_FALLTHROUGH"),
        ImmutableList.of("-pthread"),
      )

    val expectedCppOptionsResult = CppOptionsResult(ImmutableList.of(exampleExampleCppOptionsItem))

    return BazelBspTestScenarioStep("cpp options") {
      testClient.testCppOptions(20.seconds, cppOptionsParams, expectedCppOptionsResult)
    }
  }
}
