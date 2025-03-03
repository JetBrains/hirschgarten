package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.executioncontext.api.ExecutionContextListEntity
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.model.sections.ProjectViewBuildFlagsSection

data class BuildFlagsSpec(override val values: List<String>) : ExecutionContextListEntity<String>()

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
