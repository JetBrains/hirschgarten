package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.ImportRunConfigurationsSpec

internal object ImportRunConfigurationsSpecExtractor : ExecutionContextEntityExtractor<ImportRunConfigurationsSpec> {
  override fun fromProjectView(projectView: ProjectView): ImportRunConfigurationsSpec =
    ImportRunConfigurationsSpec(projectView.importRunConfigurations?.values ?: emptyList())
}
