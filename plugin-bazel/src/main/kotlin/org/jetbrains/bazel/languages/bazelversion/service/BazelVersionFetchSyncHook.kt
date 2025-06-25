package org.jetbrains.bazel.languages.bazelversion.service

import com.intellij.openapi.components.service
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.sync.ProjectPostSyncHook

class BazelVersionFetchSyncHook : ProjectPostSyncHook {
  override suspend fun onPostSync(environment: ProjectPostSyncHook.ProjectPostSyncHookEnvironment) {
    val workspaceDir = environment.project.rootDir.toNioPath()
    val currentVersion = BazelVersionWorkspaceResolver.resolveBazelVersionFromWorkspace(workspaceDir)
    environment.project
      .service<BazelVersionCheckerService>()
      .refreshLatestBazelVersion(environment.project, currentVersion)
  }
}
