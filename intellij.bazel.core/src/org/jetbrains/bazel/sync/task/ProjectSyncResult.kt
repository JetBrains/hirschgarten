package org.jetbrains.bazel.sync.task

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bsp.protocol.RawBuildTarget

internal data class ProjectSyncResult(
  val completionResult: ProjectSyncCompletionResult,
  val statistics: ProjectSyncStatistics? = null,
  val phaseDurations: List<ProjectSyncPhaseDuration> = emptyList(),
  val failureCause: Throwable? = null,
)

internal enum class ProjectSyncCompletionResult {
  SUCCESS,
  PARTIAL_SUCCESS,
  FAILURE,
  CANCELLED,
  SKIPPED,
}

internal enum class ProjectSyncPhase {
  COLLECT_PROJECT_DETAILS,
  APPLY_PROJECT_MODEL,
}

internal data class ProjectSyncPhaseDuration(
  val phase: ProjectSyncPhase,
  val durationMs: Long,
)

internal data class ProjectSyncStatistics(
  val resolvedTargetCount: Int,
  val targetLanguageClasses: List<Set<LanguageClass>>,
)

internal suspend fun <T> MutableList<ProjectSyncPhaseDuration>.trackSyncPhase(
  phase: ProjectSyncPhase,
  action: suspend () -> T,
): T {
  val startedAt = System.currentTimeMillis()
  try {
    return action()
  }
  finally {
    add(ProjectSyncPhaseDuration(phase, System.currentTimeMillis() - startedAt))
  }
}

internal fun List<RawBuildTarget>.syncStatistics(): ProjectSyncStatistics {
  val targetLanguageClasses = map { (_, _, kind) -> kind.languageClasses }
  return ProjectSyncStatistics(size, targetLanguageClasses)
}
