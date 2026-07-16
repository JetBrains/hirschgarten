package org.jetbrains.bazel.workspace.importer

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.languages.jvm.JvmDependency
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import org.jetbrains.bsp.protocol.LibraryItem
import java.nio.file.Path

@ApiStatus.Internal
data class JvmResolvedTarget(
  val key: WorkspaceTargetKey,
  val libraries: List<LibraryItem>,
  val jvmDependencies: List<JvmDependency>,
  val javaHome: Path?,
  val javaVersion: String,
)
