package org.jetbrains.bazel.cpp.sync

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.workspacecontext.WorkspaceContext

class CppProjectSync : ProjectSyncHook {
  override fun isEnabled(project: Project): Boolean = true

  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) {
    val project = environment.project
    val workspaceContext: WorkspaceContext =
      query("workspace/context") {
        environment.server.workspaceContext()
      }

    val cWorkspace = BazelCWorkspace(project)
    cWorkspace.update(workspaceContext)
  }
}
