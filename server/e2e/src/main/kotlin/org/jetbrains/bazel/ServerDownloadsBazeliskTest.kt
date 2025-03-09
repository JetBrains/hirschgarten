package org.jetbrains.bazel

import org.jetbrains.bazel.base.BazelBspTestBaseScenario
import org.jetbrains.bazel.base.BazelBspTestScenarioStep
import org.jetbrains.bazel.install.Install
import org.jetbrains.bazel.install.cli.CliOptions
import org.jetbrains.bazel.install.cli.ProjectViewCliOptions
import org.jetbrains.bsp.protocol.WorkspaceBuildTargetsResult
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes

object ServerDownloadsBazeliskTest : BazelBspTestBaseScenario() {
  private val mockTestClient = createTestkitClient()

  @JvmStatic
  fun main(args: Array<String>) = executeScenario()

  override fun installServer() {
    // DO NOT supply the -b flag to test whether bazelisk is downloaded
    Install.runInstall(
      CliOptions(
        workspaceDir = Path(workspaceDir),
        projectViewCliOptions =
          ProjectViewCliOptions(
            bazelBinary = null,
            targets = listOf("//..."),
          ),
      ),
    )
  }

  override fun scenarioSteps(): List<BazelBspTestScenarioStep> = listOf(resolveProject())

  override fun expectedWorkspaceBuildTargetsResult(): WorkspaceBuildTargetsResult = WorkspaceBuildTargetsResult(listOf())

  private fun resolveProject(): BazelBspTestScenarioStep =
    BazelBspTestScenarioStep(
      "resolve project",
    ) { mockTestClient.testResolveProject(2.minutes) }
}
