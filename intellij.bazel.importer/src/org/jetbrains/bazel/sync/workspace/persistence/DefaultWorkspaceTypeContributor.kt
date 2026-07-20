package org.jetbrains.bazel.sync.workspace.persistence

import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.commons.RepoMapping
import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.commons.TargetKind
import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bazel.label.DependencyLabelKind
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.HeavyWorkspaceTarget
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.PersistentWorkspaceSnapshot
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.PartialWorkspaceTarget
import org.jetbrains.bazel.sync.workspace.persistence.mvstore.WorkspaceTargetDeps
import org.jetbrains.bazel.sync.workspace.snapshot.CommonWorkspaceSyncConfig
import org.jetbrains.bazel.sync.workspace.snapshot.LabelConfigKey
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfiguration
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationId
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceConfigurationSummary
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSnapshotMetadata
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetGraphImpl
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.SourceFileCollection
import org.jetbrains.bsp.protocol.StrictDependencyCheckedType

internal class DefaultWorkspaceTypeContributor : WorkspaceTypeContributor {
  override fun contribute(project: Project): List<WorkspaceTypeEntry> = buildList {
    // blob and MVStore envelope types
    type<PersistentWorkspaceSnapshot>()
    type<PartialWorkspaceTarget>()
    type<WorkspaceTargetDeps>()
    type<HeavyWorkspaceTarget>()

    // configuration
    type<WorkspaceConfigurationId>()
    type<WorkspaceConfiguration>()
    type<WorkspaceConfigurationSummary>()

    // graph
    type<LabelConfigKey>()
    type<WorkspaceTargetGraphImpl>()

    // core types
    type<DependencyLabel>()
    type<DependencyLabelKind>()
    type<WorkspaceTargetKey>()
    type<WorkspaceSnapshotMetadata>()
    type<CommonWorkspaceSyncConfig>()
    type<TargetKind>()
    type<RuleType>()
    type<LanguageClass>()
    sealed<RepoMapping>()

    // TrieSourceFileCollection/TrieNode are NOT contributed: they are registered as builtins with a
    // handwritten serializer (see SnapshotSerializers.registerBuiltinTypes) - contributing them here
    // would re-register the class with the default reflective serializer
    type<SourceFileCollection>()

    type<StrictDependencyCheckedType>()
  }
}
