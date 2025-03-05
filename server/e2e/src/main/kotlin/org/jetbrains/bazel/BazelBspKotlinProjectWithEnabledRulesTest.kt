package org.jetbrains.bazel

import org.jetbrains.bazel.install.Install
import org.jetbrains.bazel.install.cli.CliOptions
import org.jetbrains.bazel.install.cli.ProjectViewCliOptions
import kotlin.io.path.Path

object BazelBspKotlinProjectWithEnabledRulesTest : BazelBspKotlinProjectTest() {
  @JvmStatic
  fun main(args: Array<String>): Unit = executeScenario()

  override fun installServer() {
    Install.runInstall(
      CliOptions(
        workspaceDir = Path(workspaceDir),
        projectViewCliOptions =
          ProjectViewCliOptions(
            bazelBinary = Path(bazelBinary),
            targets = listOf("//..."),
            directories = listOf(workspaceDir),
            shardSync = true,
            targetShardSize = 1,
            enabledRules = listOf("rules_kotlin"),
          ),
      ),
    )
  }
}
