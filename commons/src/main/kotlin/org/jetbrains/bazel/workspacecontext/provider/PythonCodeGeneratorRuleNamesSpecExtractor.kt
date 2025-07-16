package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.PythonCodeGeneratorRuleNamesSpec

object PythonCodeGeneratorRuleNamesSpecExtractor : WorkspaceContextEntityExtractor<PythonCodeGeneratorRuleNamesSpec> {
  override fun fromProjectView(projectView: ProjectView): PythonCodeGeneratorRuleNamesSpec =
    PythonCodeGeneratorRuleNamesSpec(projectView.pythonCodeGeneratorRuleNamesSection?.values ?: emptyList())
}
