package org.jetbrains.bazel.fus

import com.intellij.internal.statistic.StructuredIdeActivity
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.commons.LanguageClass
import org.jetbrains.bazel.sync.scope.FirstPhaseSync
import org.jetbrains.bazel.sync.scope.PartialProjectSync
import org.jetbrains.bazel.sync.scope.ProjectSyncScope
import org.jetbrains.bazel.sync.scope.SecondPhaseSync
import org.jetbrains.bazel.sync.task.ProjectSyncCompletionResult
import org.jetbrains.bazel.sync.task.ProjectSyncPhase
import org.jetbrains.bazel.sync.task.ProjectSyncResult
import org.jetbrains.bazel.sync.task.ProjectSyncStatistics
import java.util.concurrent.CancellationException

internal object BazelSyncCollector : CounterUsagesCollector() {
  private val GROUP = EventLogGroup("bazel.sync", 1, "FUS")

  private val SYNC_PHASE = EventFields.Enum("sync_phase", SyncPhase::class.java, description = "Which sync phase was run (first/second phase or a partial sync).")
  private val BUILD_PROJECT = EventFields.Boolean("build_project", description = "Whether the sync also built the project's targets.")
  private val COMPLETION_RESULT =
    EventFields.Enum("completion_result", ProjectSyncCompletionResult::class.java, description = "How the sync finished (success, partial success, failure, cancelled or skipped).")
  private val PHASE = EventFields.Enum("phase", ProjectSyncPhase::class.java, description = "Which internal sync phase the duration is reported for.")
  private val RESOLVED_TARGET_COUNT = EventFields.RoundedInt("resolved_target_count", description = "Number of targets resolved during the sync, rounded.")
  private val LANGUAGE =
    EventFields.StringValidatedByCustomRule("language", BazelSyncLanguageValidationRule::class.java, description = "A language discovered in the resolved Bazel target set.")
  private val LANGUAGE_TARGET_COUNT = EventFields.RoundedInt("target_count", description = "Number of resolved targets attributed to the language, rounded.")

  private val SYNC_ACTIVITY = GROUP.registerIdeActivity(
    "sync",
    startEventAdditionalFields = arrayOf(SYNC_PHASE, BUILD_PROJECT),
    finishEventAdditionalFields = arrayOf(COMPLETION_RESULT, RESOLVED_TARGET_COUNT),
  )

  private val LANGUAGE_DISCOVERED = GROUP.registerEvent(
    "language.discovered",
    EventFields.IdeActivityIdField,
    LANGUAGE,
    LANGUAGE_TARGET_COUNT,
  )

  private val PHASE_FINISHED = GROUP.registerVarargEvent(
    "phase.finished",
    EventFields.IdeActivityIdField,
    PHASE,
    EventFields.DurationMs,
  )

  override fun getGroup(): EventLogGroup = GROUP

  @Suppress("IncorrectCancellationExceptionHandling")
  suspend fun logSync(
    project: Project,
    syncScope: ProjectSyncScope,
    buildProject: Boolean,
    action: suspend () -> ProjectSyncResult,
  ): ProjectSyncResult {
    val activity = logSyncStarted(project, syncScope.fusPhase(), buildProject)
    return try {
      action().also { result -> logSyncFinished(project, activity, result) }
    }
    catch (e: CancellationException) {
      ProjectSyncResult(ProjectSyncCompletionResult.CANCELLED, failureCause = e)
        .also { result -> logSyncFinished(project, activity, result) }
    }
    catch (e: Exception) {
      ProjectSyncResult(ProjectSyncCompletionResult.FAILURE, failureCause = e)
        .also { result -> logSyncFinished(project, activity, result) }
    }
  }

  fun logSyncSkipped(project: Project, syncScope: ProjectSyncScope, buildProject: Boolean) {
    val activity = logSyncStarted(project, syncScope.fusPhase(), buildProject)
    logSyncFinished(project, activity, ProjectSyncResult(ProjectSyncCompletionResult.SKIPPED))
  }

  private fun logSyncStarted(project: Project, phase: SyncPhase, buildProject: Boolean): StructuredIdeActivity =
    SYNC_ACTIVITY.started(project) {
      listOf(
        SYNC_PHASE.with(phase),
        BUILD_PROJECT.with(buildProject),
      )
    }

  private fun logSyncFinished(
    project: Project,
    activity: StructuredIdeActivity,
    result: ProjectSyncResult,
  ) {
    result.phaseDurations.forEach { duration ->
      logPhaseFinished(project, activity, duration.phase, duration.durationMs)
    }
    activity.finished {
      buildList {
        add(COMPLETION_RESULT.with(result.completionResult))
        result.statistics?.let { add(RESOLVED_TARGET_COUNT.with(it.resolvedTargetCount)) }
      }
    }
    result.statistics?.languageTargetCountsForReporting()
      ?.filterValues { it > 0 }
      ?.forEach { (language, count) ->
        LANGUAGE_DISCOVERED.log(project, activity, language.languageName, count)
      }
  }

  private fun logPhaseFinished(project: Project, activity: StructuredIdeActivity, phase: ProjectSyncPhase, durationMs: Long) {
    PHASE_FINISHED.log(
      project,
      EventFields.IdeActivityIdField.with(activity),
      PHASE.with(phase),
      EventFields.DurationMs.with(durationMs),
    )
  }

  enum class SyncPhase {
    FIRST_PHASE,
    SECOND_PHASE,
    PARTIAL,
  }

}

private fun ProjectSyncScope.fusPhase(): BazelSyncCollector.SyncPhase =
  when (this) {
    FirstPhaseSync -> BazelSyncCollector.SyncPhase.FIRST_PHASE
    SecondPhaseSync -> BazelSyncCollector.SyncPhase.SECOND_PHASE
    is PartialProjectSync -> BazelSyncCollector.SyncPhase.PARTIAL
  }

private fun ProjectSyncStatistics.languageTargetCountsForReporting(): Map<LanguageClass, Int> =
  buildMap {
    for (languageClasses in targetLanguageClasses) {
      for (language in languageClasses.fusLanguages()) {
        put(language, getOrDefault(language, 0) + 1)
      }
    }
  }

private fun Set<LanguageClass>.fusLanguages(): Set<LanguageClass> =
  primaryFusLanguages[this] ?: this

private val primaryFusLanguages: Map<Set<LanguageClass>, Set<LanguageClass>> =
  mapOf(
    setOf(LanguageClass.JAVA, LanguageClass.KOTLIN) to setOf(LanguageClass.KOTLIN),
    setOf(LanguageClass.JAVA, LanguageClass.SCALA) to setOf(LanguageClass.SCALA),
  )
