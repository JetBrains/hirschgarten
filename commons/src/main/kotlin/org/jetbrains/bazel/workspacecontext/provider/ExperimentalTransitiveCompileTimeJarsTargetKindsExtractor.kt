package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.TransitiveCompileTimeJarsTargetKindsSpec

internal object ExperimentalTransitiveCompileTimeJarsTargetKindsExtractor :
  WorkspaceContextEntityExtractor<TransitiveCompileTimeJarsTargetKindsSpec> {
  override fun fromProjectView(projectView: ProjectView): TransitiveCompileTimeJarsTargetKindsSpec =
    TransitiveCompileTimeJarsTargetKindsSpec(projectView.transitiveCompileTimeJarsTargetKinds?.values.orEmpty())
}
