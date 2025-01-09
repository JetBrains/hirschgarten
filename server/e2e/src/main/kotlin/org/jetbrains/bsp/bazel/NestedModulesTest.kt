package org.jetbrains.bsp.bazel

import ch.epfl.scala.bsp4j.SourcesParams
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.future.await
import org.jetbrains.bazel.commons.label.Label
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bsp.bazel.install.Install
import kotlin.io.path.Path
import kotlin.io.path.relativeTo
import kotlin.time.Duration.Companion.seconds

object NestedModulesTest : BazelBspTestBaseScenario() {
  private val testClient = createTestkitClient()

  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun installServer() {
    Install.main(
      arrayOf(
        "-d",
        workspaceDir,
        "-b",
        bazelBinary,
        "-t",
        "@//...",
        "-t",
        "@inner//...",
      ),
    )
  }

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> =
    listOf(
      compareWorkspaceTargetsResults(),
    )

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult {
    error("not needed")
  }

  private fun compareWorkspaceTargetsResults(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "compare workspace targets results",
    ) {
      testClient.test(60.seconds) { session, _ ->
        val targetsResult = session.server.workspaceBuildTargets().await()

        targetsResult.targets.size shouldBe 4
        targetsResult.targets.map { Label.parse(it.id.uri) } shouldContainExactlyInAnyOrder
          listOf(
            Label.parse("@@inner$bzlmodRepoNameSeparator//:lib_inner"),
            Label.parse("@@inner$bzlmodRepoNameSeparator//:bin_inner"),
            Label.parse("@//:lib_outer"),
            Label.parse("@//:bin_outer"),
          )

        val sourcesResult =
          session.server
            .buildTargetSources(
              SourcesParams(targetsResult.targets.map { it.id }),
            ).await()

        sourcesResult.items.size shouldBe 4

        sourcesResult.items
          .flatMap {
            it.sources
          }.map { Path(it.uri.removePrefix("file:")).relativeTo(Path(workspaceDir)).toString() } shouldContainExactlyInAnyOrder
          listOf(
            "BinOuter.java",
            "LibOuter.java",
            "inner/BinInner.java",
            "inner/LibInner.java",
          )
      }
    }
}
