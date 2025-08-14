package org.jetbrains.bazel

import org.jetbrains.bazel.base.BazelBaseTestRunner
import org.jetbrains.bazel.install.cli.ProjectViewCliOptions

object BazelGazelleTestRunner : BazelBaseTestRunner() {

  override fun projectViewCliOptions(): ProjectViewCliOptions =
    super.projectViewCliOptions().copy(
      gazelleTarget = "//:gazelle"
    )

  // TODO: https://youtrack.jetbrains.com/issue/BAZEL-95
  @JvmStatic
  fun main(args: Array<String>) {
    performTest()
  }
}

