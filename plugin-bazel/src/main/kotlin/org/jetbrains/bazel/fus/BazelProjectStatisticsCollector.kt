@file:Suppress("UnstableApiUsage")

package org.jetbrains.plugins.bsp.fus

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.bspProjectProperties
import org.jetbrains.bazel.target.targetUtils

class BazelProjectStatisticsCollector : ProjectUsagesCollector() {

  override fun getGroup(): EventLogGroup {
    return GROUP
  }

  override fun getMetrics(project: Project): Set<MetricEvent> {
    val targetUtils = project.targetUtils
    val projectProperties = project.bspProjectProperties

    return if (! projectProperties.isBspProject) emptySet()
    else setOf(
      COUNT_TARGETS.metric(targetUtils.labelToTargetInfo.size),
      COUNT_FILES.metric(targetUtils.fileToTarget.size),
    )
  }

  /**
   * Recording a tool window activation event with two fields.
   */
  companion object {
    private val GROUP = EventLogGroup("bsp.project.statistics", 0, "FUS", "General statistics about a project's 'demographics'")

    private val COUNT_TARGETS = GROUP.registerEvent("count.targets", EventFields.LogarithmicInt("count_targets", "number of targets synced"))
    private val COUNT_FILES = GROUP.registerEvent("count.files", EventFields.LogarithmicInt("count_files", "number of files synced"))
  }
}
