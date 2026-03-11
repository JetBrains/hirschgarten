package org.jetbrains.bazel.languages.projectview

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.ExcludableValue
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.sync.environment.getProjectRootDirOrThrow
import org.jetbrains.bazel.sync.environment.projectCtx
import org.jetbrains.bazel.workspace.BazelExecutableProvider
import org.jetbrains.bazel.workspace.WorkspaceContextProvider
import org.jetbrains.bazel.workspacecontext.WorkspaceContext
import java.nio.file.Path
import kotlin.io.path.pathString

internal class ProjectViewWorkspaceContextProvider : WorkspaceContextProvider {
  override suspend fun computeWorkspaceContext(project: Project, bazelExecutable: Path): WorkspaceContext {
    val projectView = ProjectViewService.getInstance(project).getProjectView()
    return ProjectViewToWorkspaceContextConverter.convert(project, projectView, bazelExecutable)
  }
}
