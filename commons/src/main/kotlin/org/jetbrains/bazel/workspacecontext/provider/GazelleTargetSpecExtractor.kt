package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.GazelleTargetSpec

internal object GazelleTargetSpecExtractor : WorkspaceContextEntityExtractor<GazelleTargetSpec> {
  override fun fromProjectView(projectView: ProjectView): GazelleTargetSpec =
    GazelleTargetSpec(projectView.gazelleTarget?.value?.let { Label.parseOrNull(it) })
}
