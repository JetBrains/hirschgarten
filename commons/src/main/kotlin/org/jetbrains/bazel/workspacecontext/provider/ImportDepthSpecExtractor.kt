package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.ImportDepthSpec

private val default = ImportDepthSpec(-1)

internal object ImportDepthSpecExtractor : WorkspaceContextEntityExtractor<ImportDepthSpec> {
  override fun fromProjectView(projectView: ProjectView): ImportDepthSpec =
    when (projectView.importDepth) {
      null -> default
      else -> ImportDepthSpec(projectView.importDepth!!.value)
    }
}
