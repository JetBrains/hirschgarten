package org.jetbrains.bsp.bazel.workspacecontext

import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextListEntity
import org.jetbrains.bsp.bazel.projectview.model.ProjectView
import org.jetbrains.bsp.bazel.projectview.model.sections.ProjectViewSyncFlagsSection

data class SyncFlagsSpec(override val values: List<String>) : ExecutionContextListEntity<String>()

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
