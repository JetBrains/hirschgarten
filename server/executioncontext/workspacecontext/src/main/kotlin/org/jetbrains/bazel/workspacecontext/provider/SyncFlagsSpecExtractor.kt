package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.model.sections.ProjectViewSyncFlagsSection
import org.jetbrains.bazel.workspacecontext.SyncFlagsSpec

private val defaultBuildFlagsSpec =
  SyncFlagsSpec(
    values = emptyList(),
  )

internal object SyncFlagsSpecExtractor :
  ExecutionContextEntityExtractor<SyncFlagsSpec> {
  override fun fromProjectView(projectView: ProjectView): SyncFlagsSpec =
    projectView.syncFlags?.let {
      mapNotEmptySection(it)
    } ?: defaultBuildFlagsSpec

  private fun mapNotEmptySection(targetsSection: ProjectViewSyncFlagsSection): SyncFlagsSpec =
    SyncFlagsSpec(
      values = targetsSection.values,
    )
}
