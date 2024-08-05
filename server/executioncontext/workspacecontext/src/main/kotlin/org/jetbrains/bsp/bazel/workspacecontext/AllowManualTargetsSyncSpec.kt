package org.jetbrains.bsp.bazel.workspacecontext

import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextSingletonEntity
import org.jetbrains.bsp.bazel.projectview.model.ProjectView
import org.jetbrains.bsp.bazel.projectview.model.sections.ProjectViewAllowManualTargetsSyncSection

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
