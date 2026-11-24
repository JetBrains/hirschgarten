package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelStatus
import java.nio.file.Path

@ApiStatus.Internal
data class RunResult(
  val taskId: TaskId,
  /**
   * Exit code of the whole program run.
   * If the build wasn't successful, it's one of Bazel output codes and can be parsed to [buildStatus].
   * Otherwise, it can be any exit code that the underlying binary outputs, so it has to be an [Int].
   */
  val exitCode: Int,
  /** the text proto files of the `intellij-info` output group */
  val infoFiles: Set<Path> = emptySet(),
  /** The result of the Bazel build that always precedes a run.  */
  val buildStatus: BazelStatus,
)
