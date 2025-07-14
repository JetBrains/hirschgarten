package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.IdeJavaHomeOverrideSpec

internal object IdeJavaHomeOverrideSpecExtractor : WorkspaceContextEntityExtractor<IdeJavaHomeOverrideSpec> {
  override fun fromProjectView(projectView: ProjectView): IdeJavaHomeOverrideSpec =
    IdeJavaHomeOverrideSpec(projectView.ideJavaHomeOverride?.value)
}
