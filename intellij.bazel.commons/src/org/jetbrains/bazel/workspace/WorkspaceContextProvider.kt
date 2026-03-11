package org.jetbrains.bazel.workspace

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import java.nio.file.Path

@ApiStatus.Internal
interface WorkspaceContextProvider {
  suspend fun computeWorkspaceContext(project: Project, bazelExecutable: Path): WorkspaceContext
}
