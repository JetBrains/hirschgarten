package org.jetbrains.bsp.bazel

import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult
import io.kotest.matchers.collections.shouldContainInOrder
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.future.await
import org.jetbrains.bsp.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bsp.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bsp.bazel.install.Install
import org.jetbrains.bsp.bazel.server.model.Label
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
        val result = session.server.workspaceBuildTargets().await()

        result.targets.size shouldBe 4
        result.targets.map { Label.parse(it.id.uri) } shouldContainInOrder
          listOf(
            Label.parse("@@inner~//:lib_inner"),
            Label.parse("@@inner~//:bin_inner"),
            Label.parse("@//:lib_outer"),
            Label.parse("@//:bin_outer"),
          )
      }
    }
}
