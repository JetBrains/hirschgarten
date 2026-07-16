package org.jetbrains.bazel.sync.workspace.languages.java

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.SourceRootOptimizationMode
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceSyncConfig
import java.nio.file.Path

// RC: every property that can affect output `JavaBazelWorkspaceImporter` shall be included here
@ApiStatus.Internal
data class JavaWorkspaceSyncConfig(
  val testSourcesPatterns: List<String>,
  val importIjars: Boolean,
  val excludeCompiledSourceCodeInsideJars: Boolean,
  val sourceRootOptimizationMode: SourceRootOptimizationMode,
  val preferClassJarsOverSourcelessJars: Boolean,
  val ideJavaHomeOverride: Path?,
) : WorkspaceSyncConfig
