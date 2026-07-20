package org.jetbrains.bazel.golang.sync

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeEntry
import org.jetbrains.bazel.sync.workspace.persistence.type

private const val GOLANG_NAMESPACE = "intellij.bazel.golang"

internal class GoWorkspaceTypeContributor : WorkspaceTypeContributor {
  override fun contribute(project: Project): List<WorkspaceTypeEntry> = buildList {
    type<GoBuildTarget>()
  }
}
