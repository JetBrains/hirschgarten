package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelStatus
import java.nio.file.Path

@ApiStatus.Internal
data class RunResult(
  val taskId: TaskId,
  val statusCode: BazelStatus,
  val stdout: String?,
  /** the text proto files of the `intellij-info` output group */
  val infoFiles: Set<Path> = emptySet(),
)
