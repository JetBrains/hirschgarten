package org.jetbrains.bazel.projectview.model.sections

import org.jetbrains.bazel.label.TargetPattern
import java.nio.file.Path

sealed class ProjectViewExcludableListSection<T>(sectionName: String) : ProjectViewListSection<T>(sectionName) {
  abstract val excludedValues: List<T>
}

data class ProjectViewTargetsSection(override val values: List<TargetPattern>, override val excludedValues: List<TargetPattern>) :
  ProjectViewExcludableListSection<TargetPattern>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "targets"
  }
}

data class ProjectViewDirectoriesSection(override val values: List<Path>, override val excludedValues: List<Path>) :
  ProjectViewExcludableListSection<Path>(SECTION_NAME) {
  companion object {
    const val SECTION_NAME = "directories"
  }
}
