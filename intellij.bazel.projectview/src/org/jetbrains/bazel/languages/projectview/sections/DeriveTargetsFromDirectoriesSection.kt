package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.DERIVE_TARGETS_FROM_DIRECTORIES_KEY
import org.jetbrains.bazel.languages.projectview.sections.presets.BooleanScalarSection

internal class DeriveTargetsFromDirectoriesSection : BooleanScalarSection() {
  override val sectionKey = DERIVE_TARGETS_FROM_DIRECTORIES_KEY
  override val doc =
    "If set to true, relevant project targets will be automatically derived from the " +
      "directories during sync. Note that directories from all transitive imports are " +
      "included."
}
