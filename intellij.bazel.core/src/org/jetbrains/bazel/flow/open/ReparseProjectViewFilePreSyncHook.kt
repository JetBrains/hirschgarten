package org.jetbrains.bazel.flow.open

import com.intellij.openapi.components.serviceAsync
import org.jetbrains.bazel.languages.projectview.ProjectViewService
import org.jetbrains.bazel.sync.ProjectPreSyncHook

internal class ReparseProjectViewFilePreSyncHook : ProjectPreSyncHook {
  override suspend fun onPreSync(environment: ProjectPreSyncHook.ProjectPreSyncHookEnvironment) {
    val projectViewService = environment.project.serviceAsync<ProjectViewService>()
    projectViewService.forceReparseCurrentProjectViewFiles()
  }
}
