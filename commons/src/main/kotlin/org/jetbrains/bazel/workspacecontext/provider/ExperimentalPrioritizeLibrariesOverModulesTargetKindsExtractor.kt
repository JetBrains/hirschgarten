package org.jetbrains.bazel.workspacecontext.provider

import org.jetbrains.bazel.projectview.model.ProjectView
import org.jetbrains.bazel.workspacecontext.PrioritizeLibrariesOverModulesTargetKindsSpec

internal object ExperimentalPrioritizeLibrariesOverModulesTargetKindsExtractor :
  WorkspaceContextEntityExtractor<PrioritizeLibrariesOverModulesTargetKindsSpec> {
  override fun fromProjectView(projectView: ProjectView): PrioritizeLibrariesOverModulesTargetKindsSpec =
    PrioritizeLibrariesOverModulesTargetKindsSpec(projectView.prioritizeLibrariesOverModulesTargetKindsSection?.values.orEmpty())
}
