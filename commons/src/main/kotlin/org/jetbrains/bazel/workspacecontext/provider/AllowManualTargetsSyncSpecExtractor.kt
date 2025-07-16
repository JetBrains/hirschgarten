package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.model.sections.ProjectViewAllowManualTargetsSyncSection
import org.jetbrains.bazel.workspacecontext.AllowManualTargetsSyncSpec

private val defaultAllowManualTargetsSyncSpec =
  AllowManualTargetsSyncSpec(
    value = false,
  )

internal object AllowManualTargetsSyncSpecExtractor : WorkspaceContextEntityExtractor<AllowManualTargetsSyncSpec> {
  override fun fromProjectView(projectView: ProjectView): AllowManualTargetsSyncSpec =
    if (projectView.allowManualTargetsSync == null) {
      defaultAllowManualTargetsSyncSpec
    } else {
      map(projectView.allowManualTargetsSync!!)
    }

  private fun map(allowManualTargetsSyncSection: ProjectViewAllowManualTargetsSyncSection): AllowManualTargetsSyncSpec =
    AllowManualTargetsSyncSpec(allowManualTargetsSyncSection.value)
}
