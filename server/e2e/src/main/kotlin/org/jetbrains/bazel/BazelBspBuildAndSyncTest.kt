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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.time.Duration.Companion.minutes

object BazelBspBuildAndSyncTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()
  private val bazelBinResolved = testClient.transformJson(bazelBinDirectory)
  private val mainJar = Path("$bazelBinResolved/src/libmain.jar")
  private val genruleShouldNotBeBuilt = Path("$bazelBinResolved/src/should_not_be_built.txt")

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
      testClient.test(timeout = 5.minutes) { session ->
        val result = session.server.workspaceBuildTargets()
        testClient.assertJsonEquals<WorkspaceBuildTargetsResult>(expectedWorkspaceBuildTargetsResult(), result)
        assertFalse(mainJar.exists())
        assertFalse(genruleShouldNotBeBuilt.exists())
      }
    }

  private fun compareBuildAndGetBuildTargetsResult(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "Compare workspace/buildAndGetBuildTargets",
    ) {
      testClient.test(timeout = 5.minutes) { session ->
        session.server.runSync(true, "originId")
        val result = session.server.workspaceBuildTargets()
        testClient.assertJsonEquals<WorkspaceBuildTargetsResult>(expectedWorkspaceBuildTargetsResult(), result)
        assertTrue(mainJar.exists())
        assertFalse(genruleShouldNotBeBuilt.exists())
      }
    }

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    val javaHome =
      Path(
        "\$BAZEL_OUTPUT_BASE_PATH/external/" +
          "rules_java${bzlmodRepoNameSeparator}${bzlmodRepoNameSeparator}toolchains${bzlmodRepoNameSeparator}remotejdk17_$javaHomeArchitecture/",
      )
    val exampleExampleJvmBuildTarget =
      JvmBuildTarget(
        javaHome = javaHome,
        javaVersion = "17",
      )

    val srcMainBuildTarget =
      RawBuildTarget(
        Label.parseCanonical("$targetPrefix//src:main"),
        listOf(),
        emptyList(),
        kind =
          TargetKind(
            kindString = "java_library",
            ruleType = RuleType.LIBRARY,
            languageClasses = setOf(LanguageClass.JAVA),
          ),
        baseDirectory = Path("\$WORKSPACE/src/"),
        data = exampleExampleJvmBuildTarget,
        sources =
          listOf(
            SourceItem(
              path = Path("\$WORKSPACE/src/Main.java"),
              generated = false,
            ),
          ),
        resources = emptyList(),
      )

    return WorkspaceBuildTargetsResult(listOf(srcMainBuildTarget))
  }
}
