package org.jetbrains.bazel.languages.starlark.repomapping

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.canonicalRepoNameToPath
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceSnapshotService
import java.nio.file.Path

@get:ApiStatus.Internal
val Project.repoMapping: RepoMapping
  get() = service<WorkspaceSnapshotService>().snapshot.value.repoMapping

@get:ApiStatus.Internal
val Project.canonicalRepoNameToPath: Map<String, Path>
  get() {
    val workspaceRoot = service<WorkspaceSnapshotService>().snapshot.value.bazelInfo.workspaceRoot
    return repoMapping.canonicalRepoNameToPath(workspaceRoot)
  }
