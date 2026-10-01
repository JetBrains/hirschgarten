package org.jetbrains.bazel.sync.workspace.languages.java

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSyncConfig

// RC: every property that can affect output `JavaBazelWorkspaceImporter` shall be included here
@ApiStatus.Internal
data class JavaWorkspaceSyncConfig(
  val testSourcesPatterns: List<String>,
  val excludeCompiledSourceCodeInsideJars: Boolean,
) : WorkspaceSyncConfig
