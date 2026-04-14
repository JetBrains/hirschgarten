package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.workspace.WorkspaceContextProvider
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import java.nio.file.Path

internal class ProjectViewWorkspaceContextProvider : WorkspaceContextProvider {
  override suspend fun computeWorkspaceContext(project: Project, bazelExecutable: Path): WorkspaceContext {
    val projectView = project.projectView()
    return ProjectViewToWorkspaceContextConverter.convert(project, projectView, bazelExecutable)
  }
}
