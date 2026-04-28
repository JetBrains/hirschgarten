package org.jetbrains.bazel.sync.workspace.snapshot

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.sync.environment.bazelProjectName
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin
import org.jetbrains.bazel.workspacecontext.WorkspaceContext

@ApiStatus.Internal
object WorkspaceSnapshotBuilder {
  suspend fun build(
    project: Project,
    workspaceContext: WorkspaceContext,
    repoMapping: RepoMapping,
    resolved: BazelResolvedWorkspace,
  ): WorkspaceSnapshot {
    val commonSyncConfig = CommonWorkspaceSyncConfig(
      projectRootDir = project.rootDir.toNioPath(),
      projectName = project.bazelProjectName,
    )
    return WorkspaceSnapshot(
      targets = resolved.targets
        .map { WorkspaceTarget(rawBuildTarget = it) }
        .associateBy { WorkspaceTargetKey(label = it.rawBuildTarget.id) },
      fileToTarget = resolved.fileToTarget
        .mapValues { (_, value) -> value.map { WorkspaceTargetKey(label = it) } },
      syncConfigs = listOf(commonSyncConfig) + LanguagePlugin.EP_NAME.extensionList
        .flatMap { it.createSyncConfigs(project, workspaceContext) },
      repoMapping = repoMapping,
      hasError = resolved.hasError,
    )
  }
}
