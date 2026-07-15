package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.languages.projectview.INDEX_ALL_FILES_IN_DIRECTORIES_KEY
import org.jetbrains.bazel.languages.projectview.sections.presets.BooleanScalarSection

internal class IndexAllFilesInDirectoriesSection : BooleanScalarSection() {
  override val sectionKey = INDEX_ALL_FILES_IN_DIRECTORIES_KEY
  override val doc = "Whether to all index files inside [ProjectViewDirectoriesSection] or just sources of targets"
}
