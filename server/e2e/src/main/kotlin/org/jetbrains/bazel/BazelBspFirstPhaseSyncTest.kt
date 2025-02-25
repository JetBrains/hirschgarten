package org.jetbrains.bazel

import kotlinx.coroutines.future.await
import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.BuildTargetCapabilities
import org.jetbrains.bsp.protocol.BuildTargetIdentifier
import org.jetbrains.bsp.protocol.SourceItem
import org.jetbrains.bsp.protocol.SourceItemKind
import org.jetbrains.bsp.protocol.SourcesItem
import org.jetbrains.bsp.protocol.SourcesParams
import org.jetbrains.bsp.protocol.SourcesResult
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsFirstPhaseParams
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import org.junit.jupiter.api.Assertions.assertFalse
import java.net.URI
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
      testClient.test(timeout = 5.minutes) { session ->
        val firstPhaseResult =
          session.server
            .workspaceBuildTargetsFirstPhase(
              WorkspaceBuildTargetsFirstPhaseParams("test origin id"),
            ).await()
        testClient.assertJsonEquals(expectedWorkspaceBuildTargetsResult(), firstPhaseResult)

        assertFalse(javaLibraryJar.exists())
        assertFalse(javaBinaryJar.exists())
        assertFalse(kotlinLibraryJar.exists())
        assertFalse(kotlinBinaryJar.exists())

        val sourcesResult = session.server.buildTargetSources(SourcesParams(expectedTargetIdentifiers())).await()
        testClient.assertJsonEquals(expectedSourcesResult(), sourcesResult)
      }
    }

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val srcJavaLibTarget =
      BuildTarget(
        BuildTargetIdentifier("//src:java-lib"),
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
      )

    val srcJavaBinaryTarget =
      BuildTarget(
        BuildTargetIdentifier("//src:java-binary"),
        tags = listOf("application"),
        languageIds = listOf("java"),
        dependencies = listOf(BuildTargetIdentifier("//src:java-lib")),
        capabilities =
          BuildTargetCapabilities(
            canCompile = true,
            canTest = false,
            canRun = true,
            canDebug = false,
          ),
      )

    val srcKotlinLibTarget =
      BuildTarget(
        BuildTargetIdentifier("//src:kt-lib"),
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
      )

    val srcKotlinBinaryTarget =
      BuildTarget(
        BuildTargetIdentifier("//src:kt-binary"),
        tags = listOf("application"),
        languageIds = listOf("kotlin"),
        dependencies = listOf(BuildTargetIdentifier("//src:kt-lib")),
        capabilities =
          BuildTargetCapabilities(
            canCompile = true,
            canTest = false,
            canRun = true,
            canDebug = false,
          ),
      )

    return WorkspaceBuildTargetsResult(listOf(srcJavaLibTarget, srcJavaBinaryTarget, srcKotlinLibTarget, srcKotlinBinaryTarget))
  }

  fun expectedSourcesResult(): SourcesResult {
    val srcJavaLibSource =
      SourcesItem(
        BuildTargetIdentifier("//src:java-lib"),
        listOf(SourceItem("file://\$WORKSPACE/src/Lib.java", SourceItemKind.FILE, false)),
        roots = listOf("file://\$WORKSPACE/src/"),
      )
    val srcJavaBinarySource =
      SourcesItem(
        BuildTargetIdentifier("//src:java-binary"),
        listOf(SourceItem("file://\$WORKSPACE/src/Main.java", SourceItemKind.FILE, false)),
        roots = listOf("file://\$WORKSPACE/src/"),
      )
    val srcKotlinLibSource =
      SourcesItem(
        BuildTargetIdentifier("//src:kt-lib"),
        listOf(SourceItem("file://\$WORKSPACE/src/Lib.kt", SourceItemKind.FILE, false)),
        roots = listOf("file://\$WORKSPACE/src/"),
      )

    val srcKotlinBinarySource =
      SourcesItem(
        BuildTargetIdentifier("//src:kt-binary"),
        listOf(SourceItem("file://\$WORKSPACE/src/Main.kt", SourceItemKind.FILE, false)),
        roots = listOf("file://\$WORKSPACE/src/"),
      )

    return SourcesResult(listOf(srcJavaLibSource, srcJavaBinarySource, srcKotlinLibSource, srcKotlinBinarySource))
  }
}
