package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.EnabledRulesSpec

internal object EnabledRulesSpecExtractor : WorkspaceContextEntityExtractor<EnabledRulesSpec> {
  override fun fromProjectView(projectView: ProjectView): EnabledRulesSpec =
    EnabledRulesSpec(values = projectView.enabledRules?.values ?: emptyList())
}
