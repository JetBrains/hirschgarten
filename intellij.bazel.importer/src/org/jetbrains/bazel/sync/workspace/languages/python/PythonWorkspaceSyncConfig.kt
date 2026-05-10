package org.jetbrains.bazel.sync.workspace.languages.python

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSyncConfig

@ApiStatus.Internal
data class PythonWorkspaceSyncConfig(
  val isPythonSupportEnabled: Boolean
) : WorkspaceSyncConfig
