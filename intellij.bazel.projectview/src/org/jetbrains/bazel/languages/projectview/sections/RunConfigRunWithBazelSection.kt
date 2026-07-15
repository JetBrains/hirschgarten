package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.RUN_CONFIG_RUN_WITH_BAZEL_KEY
import org.jetbrains.bazel.languages.projectview.SectionKey
import org.jetbrains.bazel.languages.projectview.sections.presets.BooleanScalarSection

internal class RunConfigRunWithBazelSection : BooleanScalarSection() {
  override val sectionKey: SectionKey<Boolean> = RUN_CONFIG_RUN_WITH_BAZEL_KEY
  override val doc: String =
    "If enabled, will invoke Bazel for running. This means the Bazel lock is consumed and run configurations can't be run in parallel. " +
    "This also means JVM languages don't get native IDEA coverage experience. " +
    "The advantage is that Bazel test result caching and/or Remote build execution (RBE) can be used. " +
    "Note: this setting is ignored when debugging/profiling. " +
    "The default is \"true\""
}
