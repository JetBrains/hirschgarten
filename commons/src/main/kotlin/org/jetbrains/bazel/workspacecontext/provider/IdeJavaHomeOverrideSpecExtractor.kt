package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.IdeJavaHomeOverrideSpec

internal object IdeJavaHomeOverrideSpecExtractor : ExecutionContextEntityExtractor<IdeJavaHomeOverrideSpec> {
  override fun fromProjectView(projectView: ProjectView): IdeJavaHomeOverrideSpec =
    IdeJavaHomeOverrideSpec(projectView.ideJavaHomeOverride?.value)
}
