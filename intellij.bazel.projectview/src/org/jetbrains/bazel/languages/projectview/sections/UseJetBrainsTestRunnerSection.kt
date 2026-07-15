package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.USE_JETBRAINS_TEST_RUNNER_KEY
import org.jetbrains.bazel.languages.projectview.sections.presets.BooleanScalarSection

internal class UseJetBrainsTestRunnerSection : BooleanScalarSection() {
  override val sectionKey = USE_JETBRAINS_TEST_RUNNER_KEY
  override val doc = "Whether the project is using a custom JUnit test runner by JetBrains"

  companion object {
    const val NAME = "use_jetbrains_test_runner"
  }
}
