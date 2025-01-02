package org.jetbrains.bsp.bazel.workspacecontext

import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextEntityExtractorException
import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextExcludableListEntity
import org.jetbrains.bsp.bazel.projectview.model.ProjectView
import org.jetbrains.bsp.bazel.projectview.model.sections.ProjectViewDirectoriesSection
import org.jetbrains.bsp.bazel.projectview.model.sections.ProjectViewTargetsSection
import org.jetbrains.bsp.bazel.server.model.Label
import java.nio.file.Path
import kotlin.io.path.pathString

data class TargetsSpec(override val values: List<Label>, override val excludedValues: List<Label>) :
  ExecutionContextExcludableListEntity<Label>()

private val defaultTargetsSpec =
  TargetsSpec(
    values = emptyList(),
    excludedValues = emptyList(),
  )

internal object TargetsSpecExtractor : ExecutionContextEntityExtractor<TargetsSpec> {
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
      -> throw ExecutionContextEntityExtractorException(
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
      hasEmptyIncludedValuesAndNonEmptyExcludedValues(targets) -> throw ExecutionContextEntityExtractorException(
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
      Label.parse("//...")
    } else {
      Label.parse("//" + buildDirectoryIdentifier.pathString + "/...")
    }
}
