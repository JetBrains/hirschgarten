package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.model.sections.ProjectViewDirectoriesSection
import org.jetbrains.bazel.projectview.model.sections.ProjectViewTargetsSection
import org.jetbrains.bazel.workspacecontext.TargetsSpec
import org.jetbrains.bazel.workspacecontext.WorkspaceContextEntityExtractorException
import java.nio.file.Path
import kotlin.io.path.pathString

private val defaultTargetsSpec =
  TargetsSpec(
    values = emptyList(),
    excludedValues = emptyList(),
  )

internal object TargetsSpecExtractor : WorkspaceContextEntityExtractor<TargetsSpec> {
  private const val NAME = "targets"

  override fun fromProjectView(projectView: ProjectView): TargetsSpec =
    if (projectView.deriveTargetsFromDirectories?.value == true) {
      deriveTargetsFromDirectoriesSectionTrue(projectView)
    } else {
      deriveTargetsFromDirectoriesSectionFalse(projectView)
    }

  private fun deriveTargetsFromDirectoriesSectionTrue(projectView: ProjectView): TargetsSpec {
    val directories = projectView.directories ?: return deriveTargetsFromDirectoriesSectionFalse(projectView)
    return when {
      hasEmptyIncludedValuesAndEmptyExcludedValuesDirectories(directories) ->
        deriveTargetsFromDirectoriesSectionFalse(
          projectView,
        )

      hasEmptyIncludedValuesAndNonEmptyExcludedValuesDirectories(directories)
      -> throw WorkspaceContextEntityExtractorException(
        NAME,
        "'directories' section has no included targets.",
      )

      else -> mapNotEmptyDerivedTargetSection(projectView.targets, directories)
    }
  }

  private fun deriveTargetsFromDirectoriesSectionFalse(projectView: ProjectView): TargetsSpec {
    val targets = projectView.targets ?: return defaultTargetsSpec
    return when {
      hasEmptyIncludedValuesAndEmptyExcludedValues(targets) -> defaultTargetsSpec
      hasEmptyIncludedValuesAndNonEmptyExcludedValues(targets) -> throw WorkspaceContextEntityExtractorException(
        NAME,
        "'targets' section has no included targets.",
      )

      else -> mapNotEmptyNotDerivedTargetsSection(targets)
    }
  }

  private fun hasEmptyIncludedValuesAndEmptyExcludedValues(targetsSection: ProjectViewTargetsSection): Boolean =
    targetsSection.values.isEmpty() and targetsSection.excludedValues.isEmpty()

  private fun hasEmptyIncludedValuesAndNonEmptyExcludedValues(targetsSection: ProjectViewTargetsSection): Boolean =
    targetsSection.values.isEmpty() and targetsSection.excludedValues.isNotEmpty()

  private fun hasEmptyIncludedValuesAndEmptyExcludedValuesDirectories(directoriesSection: ProjectViewDirectoriesSection): Boolean =
    directoriesSection.values.isEmpty() and directoriesSection.excludedValues.isEmpty()

  private fun hasEmptyIncludedValuesAndNonEmptyExcludedValuesDirectories(directoriesSection: ProjectViewDirectoriesSection): Boolean =
    directoriesSection.values.isEmpty() and directoriesSection.excludedValues.isNotEmpty()

  private fun mapNotEmptyNotDerivedTargetsSection(targetsSection: ProjectViewTargetsSection): TargetsSpec =
    TargetsSpec(targetsSection.values, targetsSection.excludedValues)

  private fun mapNotEmptyDerivedTargetSection(
    targetsSection: ProjectViewTargetsSection?,
    directoriesSection: ProjectViewDirectoriesSection,
  ): TargetsSpec {
    val directoriesValues = directoriesSection.values.map { mapDirectoryToTarget(it) }
    val directoriesExcludedValues = directoriesSection.excludedValues.map { mapDirectoryToTarget(it) }

    return TargetsSpec(
      targetsSection?.values.orEmpty() + directoriesValues,
      targetsSection?.excludedValues.orEmpty() + directoriesExcludedValues,
    )
  }

  private fun mapDirectoryToTarget(buildDirectoryIdentifier: Path): Label =
    if (buildDirectoryIdentifier.pathString == ".") {
      Label.Companion.parse("//...")
    } else {
      Label.Companion.parse("//" + buildDirectoryIdentifier.pathString + "/...")
    }
}
