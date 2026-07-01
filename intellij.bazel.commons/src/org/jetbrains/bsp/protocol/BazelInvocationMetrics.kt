package org.jetbrains.bsp.protocol

import org.jetbrains.annotations.ApiStatus

/**
 * The kind of top-level user operation that triggered a Bazel invocation.
 * A single sync may fan out into several invocations; in that case all of them carry [SYNC].
 */
@ApiStatus.Internal
enum class BazelInvocationContext {
  SYNC,
  BUILD,
  RUN,
  TEST,
  OTHER,
}

/**
 * Why Bazel discarded its in-server (Skyframe) analysis cache, forcing a full re-analysis.
 * Derived from the `... discarding analysis cache` message Bazel prints to stderr.
 */
@ApiStatus.Internal
enum class AnalysisCacheInvalidationCause {
  /** Build options changed between two invocations on the same server (the common, expensive case). */
  BUILD_OPTIONS_CHANGED,

  /** The cache was discarded on request (e.g. `--discard_analysis_cache`). */
  EXPLICIT_DISCARD,

  /** Bazel dropped analysis state to relieve memory pressure. */
  MEMORY_PRESSURE,

  /** The discard message was recognized but its reason could not be classified. */
  UNKNOWN,
}

/**
 * A detected discard of Bazel's analysis cache during a single invocation.
 *
 * @property changedOptionCount number of build options Bazel reported as changed, when the cause is
 *   [AnalysisCacheInvalidationCause.BUILD_OPTIONS_CHANGED] and the names were listed; `null` otherwise.
 */
@ApiStatus.Internal
data class AnalysisCacheInvalidation(
  val cause: AnalysisCacheInvalidationCause,
  val changedOptionCount: Int?,
)

/**
 * Local/remote cache effectiveness for a single invocation, taken from the Bazel `BuildMetrics` BEP event.
 * `actionsExecuted` excludes local action-cache hits but includes remote cache hits (per Bazel's definition).
 */
@ApiStatus.Internal
data class BazelActionCacheStats(
  val totalActions: Long,
  val actionsExecuted: Long,
  val localActionCacheHits: Long,
  val remoteCacheHits: Long,
  val diskCacheHits: Long,
)

/**
 * Metrics derived from a single Bazel invocation's Build Event Protocol stream, reported once per invocation.
 * Either of [analysisCacheInvalidation] / [actionCacheStats] may be `null` when the corresponding signal was
 * not observed (e.g. no discard happened, or no `BuildMetrics` event was emitted).
 */
@ApiStatus.Internal
data class BazelInvocationMetrics(
  val context: BazelInvocationContext,
  val analysisCacheInvalidation: AnalysisCacheInvalidation?,
  val actionCacheStats: BazelActionCacheStats?,
  val analysisPhaseTimeMs: Long?,
)
