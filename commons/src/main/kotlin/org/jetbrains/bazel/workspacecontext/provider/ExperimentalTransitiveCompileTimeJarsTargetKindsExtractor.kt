package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.TransitiveCompileTimeJarsTargetKindsSpec

internal object ExperimentalTransitiveCompileTimeJarsTargetKindsExtractor :
  ExecutionContextEntityExtractor<TransitiveCompileTimeJarsTargetKindsSpec> {
  override fun fromProjectView(projectView: ProjectView): TransitiveCompileTimeJarsTargetKindsSpec =
    TransitiveCompileTimeJarsTargetKindsSpec(projectView.transitiveCompileTimeJarsTargetKinds?.values.orEmpty())
}
