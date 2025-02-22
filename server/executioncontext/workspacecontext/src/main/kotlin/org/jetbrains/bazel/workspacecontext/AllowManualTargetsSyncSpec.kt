package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.executioncontext.api.ExecutionContextSingletonEntity
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.projectview.model.sections.ProjectViewAllowManualTargetsSyncSection

data class AllowManualTargetsSyncSpec(override val value: Boolean) : ExecutionContextSingletonEntity<Boolean>()

private val defaultAllowManualTargetsSyncSpec =
  AllowManualTargetsSyncSpec(
    value = false,
  )

internal object AllowManualTargetsSyncSpecExtractor : ExecutionContextEntityExtractor<AllowManualTargetsSyncSpec> {
  override fun fromProjectView(projectView: ProjectView): AllowManualTargetsSyncSpec =
    if (projectView.allowManualTargetsSync == null) {
      defaultAllowManualTargetsSyncSpec
    } else {
      map(projectView.allowManualTargetsSync!!)
    }

  private fun map(allowManualTargetsSyncSection: ProjectViewAllowManualTargetsSyncSection): AllowManualTargetsSyncSpec =
    AllowManualTargetsSyncSpec(allowManualTargetsSyncSection.value)
}
