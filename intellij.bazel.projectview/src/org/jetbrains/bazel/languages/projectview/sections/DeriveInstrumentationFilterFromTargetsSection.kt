package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.DERIVE_INSTRUMENTATION_FILTER_FROM_TARGETS_KEY
import org.jetbrains.bazel.languages.projectview.sections.presets.BooleanScalarSection

internal class DeriveInstrumentationFilterFromTargetsSection : BooleanScalarSection() {
  override val sectionKey = DERIVE_INSTRUMENTATION_FILTER_FROM_TARGETS_KEY
}
