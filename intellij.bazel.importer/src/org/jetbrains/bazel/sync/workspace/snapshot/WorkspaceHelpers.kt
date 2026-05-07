package org.jetbrains.bazel.sync.workspace.snapshot

import org.jetbrains.bazel.label.DependencyLabel
import org.jetbrains.bsp.protocol.RawBuildTarget

// TODO: remove this after moving RawBuildTarget to backend module

internal val DependencyLabel.targetKey
  get() = WorkspaceTargetKey(
    label = label,
    configuration = WorkspaceConfigurationId.of(configuration),
  )

internal val RawBuildTarget.targetKey
  get() = WorkspaceTargetKey(
    label = id,
    configuration = WorkspaceConfigurationId.of(configurationId),
  )
