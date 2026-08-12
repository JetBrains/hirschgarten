package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
data class WorkspaceDirectoriesResult(
  val includedDirectories: List<Path>,
  val excludedDirectories: List<Path>
)
