package org.jetbrains.bazel.server.bep

import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos
import org.jetbrains.bsp.protocol.BazelActionCacheStats

/**
 * Extracts local/remote cache effectiveness from a Bazel `BuildMetrics` BEP event.
 * Remote and disk cache hits are taken from per-strategy [BuildEventStreamProtos.BuildMetrics.ActionSummary.RunnerCount]
 * entries; local action-cache hits come from `ActionCacheStatistics`.
 */
internal fun BuildEventStreamProtos.BuildMetrics.toActionCacheStats(): BazelActionCacheStats {
  val summary = actionSummary
  fun runnerSum(needle: String): Long =
    summary.runnerCountList.filter { it.name.contains(needle, ignoreCase = true) }.sumOf { it.count.toLong() }
  return BazelActionCacheStats(
    totalActions = summary.actionsCreated,
    actionsExecuted = summary.actionsExecuted,
    localActionCacheHits = summary.actionCacheStatistics.hits.toLong(),
    remoteCacheHits = runnerSum("remote cache hit"),
    diskCacheHits = runnerSum("disk cache hit"),
  )
}

/** Wall time spent in the analysis phase, in milliseconds, or `null` when not reported. */
internal fun BuildEventStreamProtos.BuildMetrics.analysisPhaseTimeMsOrNull(): Long? =
  if (hasTimingMetrics()) timingMetrics.analysisPhaseTimeInMs.takeIf { it > 0 } else null
