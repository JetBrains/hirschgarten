package org.jetbrains.bazel.workspacecontext

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.executioncontext.api.ExecutionContextSingletonEntity
import org.jetbrains.bazel.projectview.model.ProjectView

data class ExperimentalAddTransitiveCompileTimeJars(override val value: Boolean) : ExecutionContextSingletonEntity<Boolean>()

internal object ExperimentalAddTransitiveCompileTimeJarsExtractor :
  ExecutionContextEntityExtractor<ExperimentalAddTransitiveCompileTimeJars> {
  override fun fromProjectView(projectView: ProjectView): ExperimentalAddTransitiveCompileTimeJars =
    ExperimentalAddTransitiveCompileTimeJars(projectView.addTransitiveCompileTimeJars?.value ?: false)
}
