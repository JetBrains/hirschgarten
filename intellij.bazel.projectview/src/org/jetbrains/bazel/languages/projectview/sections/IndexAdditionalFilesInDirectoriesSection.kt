package org.jetbrains.bazel.languages.projectview.language.sections

import org.jetbrains.bazel.languages.projectview.INDEX_ADDITIONAL_FILES_IN_DIRECTORIES_KEY
import org.jetbrains.bazel.languages.projectview.ListSection

internal class IndexAdditionalFilesInDirectoriesSection : ListSection<List<String>>() {
  override val sectionKey = INDEX_ADDITIONAL_FILES_IN_DIRECTORIES_KEY
  override val doc =
    "List of filenames to index in addition to sources, resources, and libraries defined in the project. " +
      "The filenames are scanned inside directories defined in the directories: section. " +
      "This speeds up searching for these files and can be needed for custom plugins to work on them. " +
      "Has no effect if index_all_files_in_directories is set to true"

  override fun fromRawValues(rawValues: List<String>): List<String> = rawValues
}
