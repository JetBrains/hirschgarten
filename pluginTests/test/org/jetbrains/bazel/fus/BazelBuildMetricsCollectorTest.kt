package org.jetbrains.bazel.fus

import com.intellij.internal.statistic.FUCollectorTestCase
import io.kotest.matchers.shouldBe
import org.jetbrains.bazel.taskEvents.BazelTaskEventsService
import org.jetbrains.bazel.workspace.model.test.framework.MockProjectBaseTest
import org.jetbrains.bsp.protocol.AnalysisCacheInvalidation
import org.jetbrains.bsp.protocol.AnalysisCacheInvalidationCause
import org.jetbrains.bsp.protocol.BazelActionCacheStats
import org.jetbrains.bsp.protocol.BazelInvocationContext
import org.jetbrains.bsp.protocol.BazelInvocationMetrics
import org.junit.jupiter.api.Test

class BazelBuildMetricsCollectorTest : MockProjectBaseTest() {
  private fun report(metrics: BazelInvocationMetrics) =
    FUCollectorTestCase.collectLogEvents(disposable) {
      BazelTaskEventsService.getInstance(project).onBazelInvocationMetrics(metrics)
    }

  @Test
  fun `logs analysis cache invalidation only when a discard was detected`() {
    val events =
      report(
        BazelInvocationMetrics(
          context = BazelInvocationContext.SYNC,
          analysisCacheInvalidation =
            AnalysisCacheInvalidation(AnalysisCacheInvalidationCause.BUILD_OPTIONS_CHANGED, changedOptionCount = 2),
          actionCacheStats = null,
          analysisPhaseTimeMs = 1234,
        ),
      )

    val event = events.single { it.event.id == "analysis.cache.invalidated" }
    event.event.data["context"] shouldBe "SYNC"
    event.event.data["cause"] shouldBe "BUILD_OPTIONS_CHANGED"
    events.none { it.event.id == "action.cache.summary" } shouldBe true
  }

  @Test
  fun `logs action cache summary only when build metrics were present`() {
    val events =
      report(
        BazelInvocationMetrics(
          context = BazelInvocationContext.BUILD,
          analysisCacheInvalidation = null,
          actionCacheStats =
            BazelActionCacheStats(
              totalActions = 100,
              actionsExecuted = 40,
              localActionCacheHits = 30,
              remoteCacheHits = 20,
              diskCacheHits = 10,
            ),
          analysisPhaseTimeMs = null,
        ),
      )

    val event = events.single { it.event.id == "action.cache.summary" }
    event.event.data["context"] shouldBe "BUILD"
    event.event.data.containsKey("total_actions") shouldBe true
    event.event.data.containsKey("remote_cache_hits") shouldBe true
    events.none { it.event.id == "analysis.cache.invalidated" } shouldBe true
  }

  @Test
  fun `does not log action cache summary for a build with no actions such as the sync probe build`() {
    val events =
      report(
        BazelInvocationMetrics(
          context = BazelInvocationContext.SYNC,
          // The 0-target probe build still reports a discard, which we keep, but its empty summary is noise.
          analysisCacheInvalidation =
            AnalysisCacheInvalidation(AnalysisCacheInvalidationCause.BUILD_OPTIONS_CHANGED, changedOptionCount = 1),
          actionCacheStats =
            BazelActionCacheStats(
              totalActions = 0,
              actionsExecuted = 1,
              localActionCacheHits = 0,
              remoteCacheHits = 0,
              diskCacheHits = 0,
            ),
          analysisPhaseTimeMs = 50,
        ),
      )

    events.none { it.event.id == "action.cache.summary" } shouldBe true
    events.count { it.event.id == "analysis.cache.invalidated" } shouldBe 1
  }

  @Test
  fun `logs both events when both signals are present`() {
    val events =
      report(
        BazelInvocationMetrics(
          context = BazelInvocationContext.TEST,
          analysisCacheInvalidation =
            AnalysisCacheInvalidation(AnalysisCacheInvalidationCause.EXPLICIT_DISCARD, changedOptionCount = null),
          actionCacheStats =
            BazelActionCacheStats(
              totalActions = 1,
              actionsExecuted = 1,
              localActionCacheHits = 0,
              remoteCacheHits = 0,
              diskCacheHits = 0,
            ),
          analysisPhaseTimeMs = 0,
        ),
      )

    events.count { it.event.id == "analysis.cache.invalidated" } shouldBe 1
    events.count { it.event.id == "action.cache.summary" } shouldBe 1
    events.single { it.event.id == "analysis.cache.invalidated" }.event.data["cause"] shouldBe "EXPLICIT_DISCARD"
  }

  @Test
  fun `logs nothing when neither signal is present`() {
    val events =
      report(
        BazelInvocationMetrics(
          context = BazelInvocationContext.OTHER,
          analysisCacheInvalidation = null,
          actionCacheStats = null,
          analysisPhaseTimeMs = null,
        ),
      )

    events.none { it.event.id == "analysis.cache.invalidated" || it.event.id == "action.cache.summary" } shouldBe true
  }
}
