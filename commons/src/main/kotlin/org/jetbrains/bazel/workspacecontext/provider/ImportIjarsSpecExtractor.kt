package org.jetbrains.bazel.workspacecontext.provider
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.ImportIjarsSpec

internal object ImportIjarsSpecExtractor : WorkspaceContextEntityExtractor<ImportIjarsSpec> {
  override fun fromProjectView(projectView: ProjectView): ImportIjarsSpec = ImportIjarsSpec(projectView.importIjars?.value ?: false)
}
