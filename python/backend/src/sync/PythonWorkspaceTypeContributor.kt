package com.intellij.bazel.python.backend.sync

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.python.lang.PythonBuildTarget
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeEntry
import org.jetbrains.bazel.sync.workspace.persistence.type

internal class PythonWorkspaceTypeContributor : WorkspaceTypeContributor {
  override fun contribute(project: Project): List<WorkspaceTypeEntry> = buildList {
    type<PythonBuildTarget>()
    type<PythonWorkspaceSyncConfig>()
  }
}
