package org.jetbrains.bazel.clion.sync

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeEntry
import org.jetbrains.bazel.sync.workspace.persistence.type

internal class CcWorkspaceTypeContributor : WorkspaceTypeContributor {

  override fun contribute(project: Project): List<WorkspaceTypeEntry> = buildList {
    type<ArtifactLocation>()
    type<ExecutionRootPath>()
    type<CcBuildTarget>()
    type<CcBuildTarget.RuleContext>()
    type<CcBuildTarget.CompilationContext>()
    type<CcToolchainBuildTarget>()
  }
}
