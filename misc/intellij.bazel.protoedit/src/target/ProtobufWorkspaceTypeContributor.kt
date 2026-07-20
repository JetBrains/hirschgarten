package org.jetbrains.bazel.protobuf.target

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeEntry
import org.jetbrains.bazel.sync.workspace.persistence.type

internal class ProtobufWorkspaceTypeContributor : WorkspaceTypeContributor {
  override fun contribute(project: Project): List<WorkspaceTypeEntry> = buildList {
    type<ProtobufBuildTarget>()
  }
}
