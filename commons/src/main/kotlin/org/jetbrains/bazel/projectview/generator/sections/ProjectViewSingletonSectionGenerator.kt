package org.jetbrains.bazel.projectview.generator.sections

import org.jetbrains.bazel.projectview.model.sections.ProjectViewAllowManualTargetsSyncSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewBazelBinarySection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewDeriveTargetsFromDirectoriesSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewImportDepthSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewSingletonSection

abstract class ProjectViewSingletonSectionGenerator<in T : ProjectViewSingletonSection<*>> : ProjectViewSectionGenerator<T>() {
  /**
   * Returns pretty representation of a singleton section, it means that the format looks like that:
   *
   * <section name>: <value>
   */
  override fun generatePrettyStringForNonNull(section: T): String = "${section.sectionName}: ${section.value}"
}

object ProjectViewBazelBinarySectionGenerator : ProjectViewSingletonSectionGenerator<ProjectViewBazelBinarySection>()

object ProjectViewAllowManualTargetsSyncSectionGenerator : ProjectViewSingletonSectionGenerator<ProjectViewAllowManualTargetsSyncSection>()

object ProjectViewDeriveTargetsFromDirectoriesSectionGenerator :
  ProjectViewSingletonSectionGenerator<ProjectViewDeriveTargetsFromDirectoriesSection>()

object ProjectViewImportDepthSectionGenerator : ProjectViewSingletonSectionGenerator<ProjectViewImportDepthSection>()
