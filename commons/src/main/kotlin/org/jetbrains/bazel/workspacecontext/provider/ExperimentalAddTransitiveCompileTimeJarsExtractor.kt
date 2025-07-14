package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.ExperimentalAddTransitiveCompileTimeJars

internal object ExperimentalAddTransitiveCompileTimeJarsExtractor :
  WorkspaceContextEntityExtractor<ExperimentalAddTransitiveCompileTimeJars> {
  override fun fromProjectView(projectView: ProjectView): ExperimentalAddTransitiveCompileTimeJars =
    ExperimentalAddTransitiveCompileTimeJars(projectView.addTransitiveCompileTimeJars?.value ?: false)
}
