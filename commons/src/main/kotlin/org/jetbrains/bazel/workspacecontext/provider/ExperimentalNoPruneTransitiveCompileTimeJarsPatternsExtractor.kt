package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.NoPruneTransitiveCompileTimeJarsPatternsSpec

internal object ExperimentalNoPruneTransitiveCompileTimeJarsPatternsExtractor :
  WorkspaceContextEntityExtractor<NoPruneTransitiveCompileTimeJarsPatternsSpec> {
  override fun fromProjectView(projectView: ProjectView): NoPruneTransitiveCompileTimeJarsPatternsSpec {
    val rawPatterns = projectView.noPruneTransitiveCompileTimeJarsPatternsSection?.values.orEmpty()
    return NoPruneTransitiveCompileTimeJarsPatternsSpec(rawPatterns)
  }
}
