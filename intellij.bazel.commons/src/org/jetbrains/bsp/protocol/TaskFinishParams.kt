package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.BazelStatus

@ApiStatus.Internal
data class TaskFinishParams(
  val taskId: TaskId,
  val eventTime: Long? = null,
  val message: String? = null,
  val status: BazelStatus,
  val data: TaskFinishData? = null,
)

@ApiStatus.Internal
sealed interface TaskFinishData

/**
 * Only needed for `bazel run`/`test`. Normal `bazel build` knows it's started & finished even without BEP, simply when the Bazel command exits.
 */
@ApiStatus.Internal
data object BuildFinished : TaskFinishData
