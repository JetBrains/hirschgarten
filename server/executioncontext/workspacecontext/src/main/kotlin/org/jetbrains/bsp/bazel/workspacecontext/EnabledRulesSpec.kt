package org.jetbrains.bsp.bazel.workspacecontext

import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextEntityExtractor
import org.jetbrains.bsp.bazel.executioncontext.api.ExecutionContextListEntity
import org.jetbrains.bsp.bazel.projectview.model.ProjectView

data class EnabledRulesSpec(override val values: List<String>) : ExecutionContextListEntity<String>() {
  fun isNotEmpty(): Boolean = values.isNotEmpty()
}

internal object EnabledRulesSpecExtractor : ExecutionContextEntityExtractor<EnabledRulesSpec> {
  override fun fromProjectView(projectView: ProjectView): EnabledRulesSpec =
    EnabledRulesSpec(values = projectView.enabledRules?.values ?: emptyList())
}
