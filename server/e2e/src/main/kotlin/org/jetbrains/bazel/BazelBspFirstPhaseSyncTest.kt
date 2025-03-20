package org.jetbrains.bazel

import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.label.Label
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsFirstPhaseParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.junit.jupiter.api.Assertions.assertFalse
import java.net.URI
import kotlin.io.path.exists
import kotlin.io.path.toPath
import kotlin.time.Duration.Companion.minutes

object BazelBspFirstPhaseSyncTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()
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
      testClient.test(timeout = 5.minutes) { session ->
        val firstPhaseResult =
          session.server
            .workspaceBuildTargetsFirstPhase(
              WorkspaceBuildTargetsFirstPhaseParams("test origin id"),
            )
        testClient.assertJsonEquals(expectedWorkspaceBuildTargetsResult(), firstPhaseResult)

        assertFalse(javaLibraryJar.exists())
        assertFalse(javaBinaryJar.exists())
        assertFalse(kotlinLibraryJar.exists())
        assertFalse(kotlinBinaryJar.exists())
      }
    }

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val srcJavaLibTarget =
      BuildTarget(
        Label.parse("//src:java-lib"),
        tags = listOf("library"),
        languageIds = listOf("java"),
        dependencies = emptyList(),
        capabilities =
          BuildTargetCapabilities(
            canCompile = true,
            canTest = false,
            canRun = false,
            canDebug = false,
          ),
        sources = listOf(SourceItem("file://\$WORKSPACE/src/Lib.java", false)),
        resources = emptyList(),
      )

    val srcJavaBinaryTarget =
      BuildTarget(
        Label.parse("//src:java-binary"),
        tags = listOf("application"),
        languageIds = listOf("java"),
        dependencies = listOf(Label.parse("//src:java-lib")),
        capabilities =
          BuildTargetCapabilities(
            canCompile = true,
            canTest = false,
            canRun = true,
            canDebug = false,
          ),
        sources = listOf(SourceItem("file://\$WORKSPACE/src/Main.java", false)),
        resources = emptyList(),
      )

    val srcKotlinLibTarget =
      BuildTarget(
        Label.parse("//src:kt-lib"),
        tags = listOf("library"),
        languageIds = listOf("kotlin"),
        dependencies = emptyList(),
        capabilities =
          BuildTargetCapabilities(
            canCompile = true,
            canTest = false,
            canRun = false,
            canDebug = false,
          ),
        sources = listOf(SourceItem("file://\$WORKSPACE/src/Lib.kt", false)),
        resources = emptyList(),
      )

    val srcKotlinBinaryTarget =
      BuildTarget(
        Label.parse("//src:kt-binary"),
        tags = listOf("application"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(Label.parse("//src:kt-lib")),
        capabilities =
          BuildTargetCapabilities(
            canCompile = true,
            canTest = false,
            canRun = true,
            canDebug = false,
          ),
        sources = listOf(SourceItem("file://\$WORKSPACE/src/Main.kt", false)),
        resources = emptyList(),
      )

    return WorkspaceBuildTargetsResult(listOf(srcJavaLibTarget, srcJavaBinaryTarget, srcKotlinLibTarget, srcKotlinBinaryTarget))
  }
}
