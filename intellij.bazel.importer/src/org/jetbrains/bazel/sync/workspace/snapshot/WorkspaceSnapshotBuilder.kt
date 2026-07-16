package org.jetbrains.bazel.sync.workspace.snapshot

import com.intellij.openapi.project.Project
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.config.bazelProjectName
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.importDepth
import org.jetbrains.bazel.sync.workspace.BazelResolvedWorkspace
import org.jetbrains.bazel.sync.workspace.languages.LanguagePlugin

@ApiStatus.Internal
object WorkspaceSnapshotBuilder {
  suspend fun build(
    project: Project,
    projectView: ProjectView,
    repoMapping: RepoMapping,
    resolved: BazelResolvedWorkspace,
  ): WorkspaceSnapshot {
    val workspaceRoot = project.rootDir.toNioPath()
    val commonSyncConfig = CommonWorkspaceSyncConfig(
      projectRootDir = workspaceRoot,
      projectName = project.bazelProjectName,
      importDepth = projectView.importDepth,
    )
    val targets = resolved.targets
      .map {
        WorkspaceTarget(
          targetKey = it.key,
          rawBuildTarget = it,
        )
      }
      .associateBy { it.targetKey }
    return WorkspaceSnapshot(
      targets = targets,
      configurations = resolved.configurations,
      targetGraph = WorkspaceTargetGraphBuilder.build(resolved.rootTargets, targets.values),
      fileToTarget = File2TargetMapBuilder.build(workspaceRoot = workspaceRoot, targets = targets.values),
      syncConfigs = listOf(commonSyncConfig) + LanguagePlugin.EP_NAME.extensionList
        .flatMap { it.createSyncConfigs(project, projectView) },
      repoMapping = repoMapping,
      hasError = resolved.hasError,
    )
  }
}
