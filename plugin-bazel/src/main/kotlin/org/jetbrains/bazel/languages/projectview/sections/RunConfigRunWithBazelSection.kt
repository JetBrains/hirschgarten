package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.BooleanScalarSection

class RunConfigRunWithBazelSection : BooleanScalarSection() {
  override val name: String = NAME
  override val sectionKey: SectionKey<Boolean> = KEY
  override val doc: String =
    "If enabled, will invoke Bazel for running. This means the Bazel lock is consumed and run configurations can't be run in parallel. " +
    "The advantage is that Bazel test result caching and/or Remote build execution (RBE) can be used. " +
    "Note: this setting is ignored when debugging. " +
    "The default is \"true\""
  override val default: Boolean
    get() = BazelFeatureFlags.runConfigRunWithBazel

  companion object {
    const val NAME = "run_config_run_with_bazel"
    val KEY = SectionKey<Boolean>(NAME)
  }
}
