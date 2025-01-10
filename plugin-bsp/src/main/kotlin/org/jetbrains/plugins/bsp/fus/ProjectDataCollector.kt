package org.jetbrains.plugins.bsp.fus

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.bsp.config.bspProjectProperties
import org.jetbrains.plugins.bsp.target.temporaryTargetUtils

class ProjectDataCollector : ProjectUsagesCollector() {

  override fun getGroup(): EventLogGroup? {
    return GROUP
  }

  override fun getMetrics(project: Project): Set<MetricEvent> {
    val targetUtils = project.temporaryTargetUtils
    val projectProperties = project.bspProjectProperties


    val buildtoolId = projectProperties.buildToolId?.id ?: UNKNOWN_BUILD_TOOL_ID
    val loggedBuildtoolId = if (KNOWN_BUILD_TOOL_IDS.contains(buildtoolId)) buildtoolId else UNKNOWN_BUILD_TOOL_ID

    return if (! projectProperties.isBspProject) emptySet()
    else setOf(
      BUILDTOOL.metric(loggedBuildtoolId),
      COUNT_TARGETS.metric(targetUtils.targetIdToTargetInfo.size),
      COUNT_FILES.metric(targetUtils.fileToTargetId.size),
    )
  }

  /**
   * Recording a tool window activation event with two fields.
   */
  companion object {
    private val GROUP = EventLogGroup("bsp.project", 0)

    private val UNKNOWN_BUILD_TOOL_ID = "unknown"
    private val KNOWN_BUILD_TOOL_IDS = listOf("bazelbsp", "mill", "sbt", "unknown")

    // build tool id is expected to be set by plugins only and should not contain sensitive data
    private val BUILDTOOL = GROUP.registerEvent("buildtool", EventFields.String("buildtool", KNOWN_BUILD_TOOL_IDS))
    private val COUNT_TARGETS = GROUP.registerEvent("count.targets", EventFields.LogarithmicInt("count_targets", "number of targets synced"))
    private val COUNT_FILES = GROUP.registerEvent("count.files", EventFields.LogarithmicInt("count_files", "number of files synced"))
  }
}
