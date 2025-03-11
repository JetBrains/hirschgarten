package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.model.sections.ProjectViewDirectoriesSection
import org.jetbrains.bazel.workspacecontext.DirectoriesSpec
import java.nio.file.Path

internal class DirectoriesSpecExtractor(private val workspaceRoot: Path) : ExecutionContextEntityExtractor<DirectoriesSpec> {
  override fun fromProjectView(projectView: ProjectView): DirectoriesSpec =
    projectView.directories?.toDirectoriesSpec()
      ?: DirectoriesSpec(
        values = listOf(workspaceRoot),
        excludedValues = emptyList(),
      )

  private fun ProjectViewDirectoriesSection.toDirectoriesSpec(): DirectoriesSpec =
    DirectoriesSpec(
      values = values.map { it.resolveAndNormalize() },
      excludedValues = excludedValues.map { it.resolveAndNormalize() },
    )

  private fun Path.resolveAndNormalize() = workspaceRoot.resolve(this).normalize()
}
