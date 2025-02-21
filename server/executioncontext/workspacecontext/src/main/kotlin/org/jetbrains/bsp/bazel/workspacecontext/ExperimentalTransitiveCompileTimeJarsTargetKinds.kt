package org.jetbrains.bsp.bazel.workspacecontext

import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextListEntity
import org.jetbrains.bsp.bazel.projectview.model.ProjectView

data class TransitiveCompileTimeJarsTargetKindsSpec(override val values: List<String>) : ExecutionContextListEntity<String>()

internal object ExperimentalTransitiveCompileTimeJarsTargetKindsExtractor :
  ExecutionContextEntityExtractor<TransitiveCompileTimeJarsTargetKindsSpec> {
  override fun fromProjectView(projectView: ProjectView): TransitiveCompileTimeJarsTargetKindsSpec =
    TransitiveCompileTimeJarsTargetKindsSpec(projectView.transitiveCompileTimeJarsTargetKinds?.values ?: emptyList())
}
