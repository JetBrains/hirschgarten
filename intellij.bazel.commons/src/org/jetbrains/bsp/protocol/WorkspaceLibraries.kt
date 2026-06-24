package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.snapshot.WorkspaceTargetKey
import java.nio.file.Path

@ApiStatus.Internal
data class LibraryItem(
  val key: WorkspaceTargetKey,
  val ijars: List<Path>,
  val jars: List<Path>,
  val sourceJars: List<Path>,
  val mavenCoordinates: MavenCoordinates?,
  val containsInternalJars: Boolean,
)

@get:ApiStatus.Internal
val LibraryItem.allJars: List<Path>
  get() = ijars + jars + sourceJars

@ApiStatus.Internal
data class MavenCoordinates(
  val groupId: String,
  val artifactId: String,
  val version: String,
)
