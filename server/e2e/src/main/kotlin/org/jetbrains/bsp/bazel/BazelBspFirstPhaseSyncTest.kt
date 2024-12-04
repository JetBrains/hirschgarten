package org.jetbrains.bsp.bazel

import ch.epfl.scala.bsp4j.BuildTarget
import ch.epfl.scala.bsp4j.BuildTargetCapabilities
import ch.epfl.scala.bsp4j.BuildTargetIdentifier
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import kotlinx.coroutines.future.await
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsFirstPhaseParams
import org.junit.jupiter.api.Assertions.assertFalse
import java.net.URI
import kotlin.also
import kotlin.io.path.exists
import kotlin.io.path.toPath
import kotlin.time.Duration.Companion.minutes

object BazelBspFirstPhaseSyncTest : BazelBspTestBaseScenario() {
  private val testClient = createBazelClient()
  private val bazelBinResolved = testClient.transformJson(bazelBinDirectory)

  private val javaLibraryJar = URI.create("$bazelBinResolved/src/libjava-lib.jar").toPath()
  private val javaBinaryJar = URI.create("$bazelBinResolved/src/java-binary.jar").toPath()

  private val kotlinLibraryJar = URI.create("$bazelBinResolved/src/kt-lib.jar").toPath()
  private val kotlinBinaryJar = URI.create("$bazelBinResolved/src/kt-binary.jar").toPath()

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-95
  @JvmStatic
  fun main(args: Array<String>): Unit = executeScenario()

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      compareWorkspaceBuildTargetsFirstPhase(),
    )

  private fun compareWorkspaceBuildTargetsFirstPhase(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "Compare workspace/buildTargetsFirstPhase",
    ) {
      testClient.test(timeout = 5.minutes) { session, _ ->
        val result = session.server.workspaceBuildTargetsFirstPhase(WorkspaceBuildTargetsFirstPhaseParams("test origin id")).await()
        testClient.assertJsonEquals<WorkspaceBuildTargetsResult>(
          expectedWorkspaceBuildTargetsResult(),
          result,
        )

        assertFalse(javaLibraryJar.exists())
        assertFalse(javaBinaryJar.exists())
        assertFalse(kotlinLibraryJar.exists())
        assertFalse(kotlinBinaryJar.exists())
      }
    }

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val srcJavaLibTarget =
      BuildTarget(
        BuildTargetIdentifier("//src:java-lib"),
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

    val srcJavaBinaryTarget =
      BuildTarget(
        BuildTargetIdentifier("//src:java-binary"),
        listOf("application"),
        listOf("java"),
        listOf(BuildTargetIdentifier("//src:java-lib")),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = true
          it.canDebug = false
        },
      )

    val srcKotlinLibTarget =
      BuildTarget(
        BuildTargetIdentifier("//src:kt-lib"),
        listOf("library"),
        listOf("kotlin"),
        emptyList(),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = false
          it.canDebug = false
        },
      )

    val srcKotlinBinaryTarget =
      BuildTarget(
        BuildTargetIdentifier("//src:kt-binary"),
        listOf("application"),
        listOf("kotlin"),
        listOf(BuildTargetIdentifier("//src:kt-lib")),
        BuildTargetCapabilities().also {
          it.canCompile = true
          it.canTest = false
          it.canRun = true
          it.canDebug = false
        },
      )

    return WorkspaceBuildTargetsResult(listOf(srcJavaLibTarget, srcJavaBinaryTarget, srcKotlinLibTarget, srcKotlinBinaryTarget))
  }
}
