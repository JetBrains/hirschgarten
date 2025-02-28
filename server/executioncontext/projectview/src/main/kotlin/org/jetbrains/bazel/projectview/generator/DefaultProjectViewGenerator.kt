package org.jetbrains.bazel.projectview.generator

import org.jetbrains.bazel.projectview.generator.sections.ProjectViewAllowManualTargetsSyncSectionGenerator
import org.jetbrains.bazel.projectview.generator.sections.ProjectViewBazelBinarySectionGenerator
import org.jetbrains.bazel.projectview.generator.sections.ProjectViewBuildFlagsSectionGenerator
import org.jetbrains.bazel.projectview.generator.sections.ProjectViewDeriveTargetsFromDirectoriesSectionGenerator
import org.jetbrains.bazel.projectview.generator.sections.ProjectViewDirectoriesSectionGenerator
import org.jetbrains.bazel.projectview.generator.sections.ProjectViewEnabledRulesSectionGenerator
import org.jetbrains.bazel.projectview.generator.sections.ProjectViewImportDepthSectionGenerator
import org.jetbrains.bazel.projectview.generator.sections.ProjectViewSyncFlagsSectionGenerator
import org.jetbrains.bazel.projectview.generator.sections.ProjectViewTargetsSectionGenerator
import org.jetbrains.bazel.projectview.model.ProjectView
import java.nio.file.Files
import java.nio.file.Path

object DefaultProjectViewGenerator : ProjectViewGenerator {
  override fun generatePrettyStringAndSaveInFile(projectView: ProjectView, filePath: Path) {
    Files.createDirectories(filePath.parent)
    Files.writeString(filePath, generatePrettyString(projectView))
  }

  override fun generatePrettyString(projectView: ProjectView): String =
    listOfNotNull(
      ProjectViewTargetsSectionGenerator.generatePrettyString(projectView.targets),
      ProjectViewBazelBinarySectionGenerator.generatePrettyString(projectView.bazelBinary),
      ProjectViewBuildFlagsSectionGenerator.generatePrettyString(projectView.buildFlags),
      ProjectViewSyncFlagsSectionGenerator.generatePrettyString(projectView.syncFlags),
      ProjectViewAllowManualTargetsSyncSectionGenerator.generatePrettyString(projectView.allowManualTargetsSync),
      ProjectViewDirectoriesSectionGenerator.generatePrettyString(projectView.directories),
      ProjectViewDeriveTargetsFromDirectoriesSectionGenerator.generatePrettyString(projectView.deriveTargetsFromDirectories),
      ProjectViewImportDepthSectionGenerator.generatePrettyString(projectView.importDepth),
      ProjectViewEnabledRulesSectionGenerator.generatePrettyString(projectView.enabledRules),
    ).joinToString(separator = "\n\n", postfix = "\n")
}
