package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.executioncontext.api.ExecutionContextListEntity
import org.jetbrains.bazel.projectview.model.ProjectView

data class TransitiveCompileTimeJarsTargetKindsSpec(override val values: List<String>) : ExecutionContextListEntity<String>()

internal object ExperimentalTransitiveCompileTimeJarsTargetKindsExtractor :
  ExecutionContextEntityExtractor<TransitiveCompileTimeJarsTargetKindsSpec> {
  override fun fromProjectView(projectView: ProjectView): TransitiveCompileTimeJarsTargetKindsSpec =
    TransitiveCompileTimeJarsTargetKindsSpec(projectView.transitiveCompileTimeJarsTargetKinds?.values ?: emptyList())
}
