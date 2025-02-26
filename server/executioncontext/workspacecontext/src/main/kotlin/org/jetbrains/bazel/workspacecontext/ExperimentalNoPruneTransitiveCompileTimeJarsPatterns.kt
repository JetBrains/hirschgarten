package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.executioncontext.api.ExecutionContextListEntity
import org.jetbrains.bazel.projectview.model.ProjectView

data class NoPruneTransitiveCompileTimeJarsPatternsSpec(override val values: List<String>) : ExecutionContextListEntity<String>()

internal object ExperimentalNoPruneTransitiveCompileTimeJarsPatternsExtractor :
  ExecutionContextEntityExtractor<NoPruneTransitiveCompileTimeJarsPatternsSpec> {
  override fun fromProjectView(projectView: ProjectView): NoPruneTransitiveCompileTimeJarsPatternsSpec {
    val rawPatterns = projectView.noPruneTransitiveCompileTimeJarsPatternsSection?.values.orEmpty()
    return NoPruneTransitiveCompileTimeJarsPatternsSpec(rawPatterns)
  }
}
