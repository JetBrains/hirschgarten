package org.jetbrains.bazel.sync.task

import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.sync.ProjectSyncCompletionResult
import org.jetbrains.bazel.sync.ProjectSyncOutcome

internal data class ProjectSyncResult(
  val completionResult: ProjectSyncCompletionResult,
  val statistics: ProjectSyncStatistics? = null,
  val failureCause: Throwable? = null,
) {
  fun toOutcome(): ProjectSyncOutcome = ProjectSyncOutcome(completionResult, failureCause)
}

internal enum class ProjectSyncPhase {
  COLLECT_PROJECT_DETAILS,
  APPLY_PROJECT_MODEL,
}

/**
 * @property resolvedTargetCount The number of resolved targets, or `null` when the sync stopped before it resolved the targets.
 * @property phaseDurations The duration of each sync phase that ran, in milliseconds.
 */
internal data class ProjectSyncStatistics(
  val resolvedTargetCount: Int? = null,
  val targetLanguageClasses: List<Set<LanguageClass>> = emptyList(),
  val importerDurations: Map<String, Long> = emptyMap(),
  val phaseDurations: Map<ProjectSyncPhase, Long> = emptyMap(),
)

internal fun ProjectSyncResult.withPhaseDurations(phaseDurations: Map<ProjectSyncPhase, Long>): ProjectSyncResult =
  copy(statistics = (statistics ?: ProjectSyncStatistics()).copy(phaseDurations = phaseDurations.toMap()))
