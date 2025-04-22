package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.NoPruneTransitiveCompileTimeJarsPatternsSpec

internal object ExperimentalNoPruneTransitiveCompileTimeJarsPatternsExtractor :
  ExecutionContextEntityExtractor<NoPruneTransitiveCompileTimeJarsPatternsSpec> {
  override fun fromProjectView(projectView: ProjectView): NoPruneTransitiveCompileTimeJarsPatternsSpec {
    val rawPatterns = projectView.noPruneTransitiveCompileTimeJarsPatternsSection?.values.orEmpty()
    return NoPruneTransitiveCompileTimeJarsPatternsSpec(rawPatterns)
  }
}
