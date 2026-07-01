package org.jetbrains.bazel.fus

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.eventLog.events.VarargEventId
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.project.Project
import org.jetbrains.bsp.protocol.AnalysisCacheInvalidationCause
import org.jetbrains.bsp.protocol.BazelInvocationContext
import org.jetbrains.bsp.protocol.BazelInvocationMetrics

/**
 * Reports metrics derived from a single Bazel invocation's Build Event Protocol stream:
 * - `analysis.cache.invalidated`: how often and why Bazel discards its analysis cache (forcing a full re-analysis),
 *   plus how costly the re-analysis was.
 * - `action.cache.summary`: local/remote action cache effectiveness for the invocation.
 *
 * Both events fall out of the same collection point ([org.jetbrains.bazel.taskEvents.BazelTaskEventsService]).
 */
internal object BazelBuildMetricsCollector : CounterUsagesCollector() {
  private val GROUP = EventLogGroup("bazel.build.metrics", 1, "FUS")

  private val CONTEXT = EventFields.Enum("context", BazelInvocationContext::class.java)

  private val CAUSE = EventFields.Enum("cause", AnalysisCacheInvalidationCause::class.java)
  private val CHANGED_OPTION_COUNT = EventFields.RoundedInt("changed_option_count")
  private val ANALYSIS_CACHE_INVALIDATED: VarargEventId =
    GROUP.registerVarargEvent("analysis.cache.invalidated", CONTEXT, CAUSE, CHANGED_OPTION_COUNT, EventFields.DurationMs)

  private val TOTAL_ACTIONS = EventFields.LogarithmicLong("total_actions")
  private val ACTIONS_EXECUTED = EventFields.LogarithmicLong("actions_executed")
  private val LOCAL_ACTION_CACHE_HITS = EventFields.LogarithmicLong("local_action_cache_hits")
  private val REMOTE_CACHE_HITS = EventFields.LogarithmicLong("remote_cache_hits")
  private val DISK_CACHE_HITS = EventFields.LogarithmicLong("disk_cache_hits")
  private val ACTION_CACHE_SUMMARY: VarargEventId =
    GROUP.registerVarargEvent(
      "action.cache.summary",
      CONTEXT,
      TOTAL_ACTIONS,
      ACTIONS_EXECUTED,
      LOCAL_ACTION_CACHE_HITS,
      REMOTE_CACHE_HITS,
      DISK_CACHE_HITS,
    )

  override fun getGroup(): EventLogGroup = GROUP

  fun log(project: Project, metrics: BazelInvocationMetrics) {
    metrics.analysisCacheInvalidation?.let { invalidation ->
      ANALYSIS_CACHE_INVALIDATED.log(
        project,
        CONTEXT.with(metrics.context),
        CAUSE.with(invalidation.cause),
        CHANGED_OPTION_COUNT.with(invalidation.changedOptionCount ?: 0),
        EventFields.DurationMs.with(metrics.analysisPhaseTimeMs ?: 0),
      )
    }
    // Skip the summary when nothing was built (e.g. the 0-target probe build the sync runs): no actions means
    // no cache effectiveness to report. The analysis-cache-invalidated event above is still logged when present.
    metrics.actionCacheStats?.takeIf { it.totalActions > 0 }?.let { stats ->
      ACTION_CACHE_SUMMARY.log(
        project,
        CONTEXT.with(metrics.context),
        TOTAL_ACTIONS.with(stats.totalActions),
        ACTIONS_EXECUTED.with(stats.actionsExecuted),
        LOCAL_ACTION_CACHE_HITS.with(stats.localActionCacheHits),
        REMOTE_CACHE_HITS.with(stats.remoteCacheHits),
        DISK_CACHE_HITS.with(stats.diskCacheHits),
      )
    }
  }
}
