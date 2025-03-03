package org.jetbrains.bazel

import org.jetbrains.bazel.install.Install

object BazelBspKotlinProjectWithEnabledRulesTest : BazelBspKotlinProjectTest() {
  @JvmStatic
  fun main(args: Array<String>): Unit = executeScenario()

  override fun installServer() {
    Install.main(
      arrayOf(
        "-d",
        workspaceDir,
        "-b",
        bazelBinary,
        "-t",
        "//...",
        "--enabled-rules",
        "rules_kotlin",
        "--shard-sync",
        "true",
        "--target-shard-size",
        "1",
      ),
    )
  }
}
