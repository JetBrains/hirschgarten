package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.model.sections.ProjectViewBuildFlagsSection
import org.jetbrains.bazel.workspacecontext.BuildFlagsSpec

private val defaultBuildFlagsSpec =
  BuildFlagsSpec(
    values = emptyList(),
  )

internal object BuildFlagsSpecExtractor : ExecutionContextEntityExtractor<BuildFlagsSpec> {
  override fun fromProjectView(projectView: ProjectView): BuildFlagsSpec =
    when (projectView.buildFlags) {
      null -> defaultBuildFlagsSpec
      else -> mapNotEmptySection(projectView.buildFlags!!)
    }

  private fun mapNotEmptySection(targetsSection: ProjectViewBuildFlagsSection): BuildFlagsSpec =
    BuildFlagsSpec(
      values = targetsSection.values,
    )
}
