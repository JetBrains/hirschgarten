package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.BooleanScalarSection

class UseJetBrainsTestRunnerSection : BooleanScalarSection() {
  override val name = NAME
  override val sectionKey = KEY
  override val doc = "Whether the project is using a custom JUnit test runner by JetBrains"

  companion object {
    const val NAME = "use_jetbrains_test_runner"
    val KEY = SectionKey<Boolean>(NAME)
  }
}
