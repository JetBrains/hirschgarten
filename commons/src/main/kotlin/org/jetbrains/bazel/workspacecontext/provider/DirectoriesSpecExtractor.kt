package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.model.sections.ProjectViewDirectoriesSection
import org.jetbrains.bazel.workspacecontext.DirectoriesSpec
import java.nio.file.Path

internal class DirectoriesSpecExtractor(private val workspaceRoot: Path, projectViewPath: Path) :
  WorkspaceContextEntityExtractor<DirectoriesSpec> {
  private val normalizedProjectViewPath = projectViewPath.normalize()

  override fun fromProjectView(projectView: ProjectView): DirectoriesSpec =
    projectView.directories?.toDirectoriesSpec()
      ?: DirectoriesSpec(
        values = listOf(workspaceRoot, normalizedProjectViewPath),
        excludedValues = emptyList(),
      )

  private fun ProjectViewDirectoriesSection.toDirectoriesSpec(): DirectoriesSpec =
    DirectoriesSpec(
      // the currently used project view file should not be excluded from the project
      values =
        values.map {
          it.resolveAndNormalize()
        } + listOf(normalizedProjectViewPath),
      excludedValues =
        excludedValues
          .map { it.resolveAndNormalize() }
          .filter { it != normalizedProjectViewPath },
    )

  private fun Path.resolveAndNormalize() = workspaceRoot.resolve(this).normalize()
}
