package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.DeriveInstrumentationFilterFromTargetsSpec

object DeriveInstrumentationFilterFromTargetsExtractor : WorkspaceContextEntityExtractor<DeriveInstrumentationFilterFromTargetsSpec> {
  override fun fromProjectView(projectView: ProjectView): DeriveInstrumentationFilterFromTargetsSpec =
    DeriveInstrumentationFilterFromTargetsSpec(projectView.deriveInstrumentationFilterFromTargets?.value ?: true)
}
