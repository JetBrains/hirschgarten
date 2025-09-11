package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.IndexAdditionalFilesInDirectoriesSpec

object IndexAdditionalFilesInDirectoriesSpecExtractor : WorkspaceContextEntityExtractor<IndexAdditionalFilesInDirectoriesSpec> {
  override fun fromProjectView(projectView: ProjectView): IndexAdditionalFilesInDirectoriesSpec =
    IndexAdditionalFilesInDirectoriesSpec(projectView.indexAdditionalFilesInDirectories?.values ?: emptyList())
}
