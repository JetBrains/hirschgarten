package org.jetbrains.bazel.sync

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
enum class ProjectSyncCompletionResult {
  SUCCESS,
  PARTIAL_SUCCESS,
  FAILURE,
  CANCELLED,
  SKIPPED,
}

/**
 * How a sync ended. [failureCause] is set when the sync ended with an exception.
 */
@ApiStatus.Internal
data class ProjectSyncOutcome(
  val completionResult: ProjectSyncCompletionResult,
  val failureCause: Throwable? = null,
) {
  /** A partial success counts as a success: the project model was updated, and some targets have errors. */
  val isSuccessful: Boolean
    get() = completionResult == ProjectSyncCompletionResult.SUCCESS || completionResult == ProjectSyncCompletionResult.PARTIAL_SUCCESS

  /** Throws [IllegalStateException] with the failure cause when the sync did not end in a success. */
  fun checkSuccessful() {
    if (!isSuccessful) {
      // A skipped sync writes nothing to the Sync tab.
      val reason = failureCause?.let { ": ${it.message ?: it.javaClass.name}" }
                   ?: if (completionResult == ProjectSyncCompletionResult.SKIPPED) {
                     ". The sync did not run, because another sync was in progress or the project is not trusted"
                   }
                   else {
                     ". See the Sync tab for the cause"
                   }
      throw IllegalStateException("The sync ended with $completionResult$reason", failureCause)
    }
  }
}
