package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.model.sections.ProjectViewDebugFlagsSection
import org.jetbrains.bazel.workspacecontext.DebugFlagsSpec

private val defaultBuildFlagsSpec =
  DebugFlagsSpec(
    values = emptyList(),
  )

internal object DebugFlagsSpecExtractor :
  ExecutionContextEntityExtractor<DebugFlagsSpec> {
  override fun fromProjectView(projectView: ProjectView): DebugFlagsSpec =
    projectView.debugFlags?.let {
      mapNotEmptySection(it)
    } ?: defaultBuildFlagsSpec

  private fun mapNotEmptySection(targetsSection: ProjectViewDebugFlagsSection): DebugFlagsSpec =
    DebugFlagsSpec(
      values = targetsSection.values,
    )
}
