package org.jetbrains.bsp.bazel

import kotlinx.coroutines.future.await
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.BuildTargetDataKind
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.JvmBuildTarget
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import java.net.URI
import kotlin.io.path.exists
import kotlin.io.path.toPath
import kotlin.time.Duration.Companion.minutes

object BazelBspBuildAndSyncTest : BazelBspTestBaseScenario() {
  private val testClient = createBazelClient()
  private val bazelBinResolved = testClient.transformJson(bazelBinDirectory)
  private val mainJar = URI.create("$bazelBinResolved/src/libmain.jar").toPath()
  private val genruleShouldNotBeBuilt = URI.create("$bazelBinResolved/src/should_not_be_built.txt").toPath()

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-95
  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      compareWorkspaceBuildTargets(),
      compareBuildAndGetBuildTargetsResult(),
    )

  private fun compareWorkspaceBuildTargets(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "Compare workspace/buildTargets",
    ) {
      testClient.test(timeout = 5.minutes) { session, _ ->
        val result = session.server.workspaceBuildTargets().await()
        testClient.assertJsonEquals<WorkspaceBuildTargetsResult>(expectedWorkspaceBuildTargetsResult(), result)
        assertFalse(mainJar.exists())
        assertFalse(genruleShouldNotBeBuilt.exists())
      }
    }

  private fun compareBuildAndGetBuildTargetsResult(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "Compare workspace/buildAndGetBuildTargets",
    ) {
      testClient.test(timeout = 5.minutes) { session, _ ->
        val result = session.server.workspaceBuildAndGetBuildTargets().await()
        testClient.assertJsonEquals<WorkspaceBuildTargetsResult>(expectedWorkspaceBuildTargetsResult(), result)
        assertTrue(mainJar.exists())
        assertFalse(genruleShouldNotBeBuilt.exists())
      }
    }

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val javaHome =
      "file://\$BAZEL_OUTPUT_BASE_PATH/external/" +
        "rules_java${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}toolchains${bzlmodRepoNameSeparator}remotejdk17_$javaHomeArchitecture/"
    val exampleExampleJvmBuildTarget =
      JvmBuildTarget(
        it.javaHome = javaHome
        it.javaVersion = "17"
      }

    val srcMainBuildTarget =
      BuildTarget(
        BuildTargetIdentifier("$targetPrefix//src:main"),
        listOf("library"),
        listOf("java"),
        emptyList(),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = false
          it.canDebug = false
        },
      )
    srcMainBuildTarget.displayName = "$targetPrefix//src:main"
    srcMainBuildTarget.baseDirectory = "file://\$WORKSPACE/src/"
    srcMainBuildTarget.data = exampleExampleJvmBuildTarget
    srcMainBuildTarget.dataKind = BuildTargetDataKind.JVM

    return WorkspaceBuildTargetsResult(listOf(srcMainBuildTarget))
  }
}
