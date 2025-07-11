package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.IndexAllFilesInDirectoriesSpec

internal object IndexAllFilesInDirectoriesSpecExtractor : ExecutionContextEntityExtractor<IndexAllFilesInDirectoriesSpec> {
  override fun fromProjectView(projectView: ProjectView): IndexAllFilesInDirectoriesSpec =
    IndexAllFilesInDirectoriesSpec(projectView.indexAllFilesInDirectories?.value ?: false)
}
