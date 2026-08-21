package org.jetbrains.bazel.clion.sync

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeContributor
import org.jetbrains.bazel.sync.workspace.persistence.WorkspaceTypeEntry
import org.jetbrains.bazel.sync.workspace.persistence.type
import java.nio.file.Path

/**
 * Marker class for paths that are either absolute (non-hermetic) or relative
 * to the execution root.
 */
@JvmInline
value class ExecutionRootPath(val path: Path) {

  constructor(path: String) : this(Path.of(path))
}

internal class ExecutionRootPathWorkspaceTypeContributor : WorkspaceTypeContributor {

  override fun contribute(project: Project): List<WorkspaceTypeEntry> = buildList { type<ExecutionRootPath>() }
}
