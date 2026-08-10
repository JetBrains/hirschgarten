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
import org.jetbrains.bazel.sync.workspace.persistence.InMemoryWorkspaceTargetMap

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
    val targets = resolved.targets.associateBy { it.key }
    val targetGraph = WorkspaceTargetGraphBuilder.build(resolved.rootTargets, targets.values)
    return WorkspaceSnapshot(
      targets = InMemoryWorkspaceTargetMap(targets),
      configurations = resolved.configurations,
      targetGraph = targetGraph,
      fileToTarget = File2TargetMapBuilder.build(targets = targets.values),
      executableTargets = ExecutableTargetsIndexBuilder.build(
        targetGraph = targetGraph,
        importDepth = commonSyncConfig.importDepth,
        targets = targets.values,
      ),
      syncConfigs = listOf(commonSyncConfig) + LanguagePlugin.EP_NAME.extensionList
        .flatMap { it.createSyncConfigs(project, projectView) },
      repoMapping = repoMapping,
      metadata = WorkspaceSnapshotMetadata(
        version = 1,
      ),
    )
  }
}
