package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.projectview.IMPORT_DEPTH_KEY
import org.jetbrains.bazel.languages.projectview.sections.presets.IntScalarSection

@ApiStatus.Internal
class ImportDepthSection : IntScalarSection() {
  override val sectionKey = IMPORT_DEPTH_KEY
  override val doc =
    "Specifies how many levels of Bazel targets' dependencies should be imported as modules. " +
      "Only the targets that are present in the workspace are imported. You can use a negative value to import all transitive dependencies. " +
      "The default value is -1, meaning that all transitive context is included."
}
