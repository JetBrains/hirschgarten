package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.AndroidMinSdkSpec

internal object AndroidMinSdkSpecExtractor : WorkspaceContextEntityExtractor<AndroidMinSdkSpec> {
  override fun fromProjectView(projectView: ProjectView): AndroidMinSdkSpec = AndroidMinSdkSpec(projectView.androidMinSdkSection?.value)
}
