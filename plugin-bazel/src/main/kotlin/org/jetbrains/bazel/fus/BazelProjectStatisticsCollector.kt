@file:Suppress("UnstableApiUsage")

package org.jetbrains.bazel.fus

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.bazelProjectProperties
import org.jetbrains.bazel.target.targetUtils

class BazelProjectStatisticsCollector : ProjectUsagesCollector() {
  override fun getGroup(): EventLogGroup = Const.GROUP

  override fun getMetrics(project: Project): Set<MetricEvent> {
    val targetUtils = project.targetUtils
    val projectProperties = project.bazelProjectProperties

    return if (projectProperties.isBazelProject) {
      setOf(
        Const.COUNT_TARGETS.metric(targetUtils.labelToTargetInfo.size),
        Const.COUNT_FILES.metric(targetUtils.fileToTarget.size),
      )
    } else {
      emptySet()
    }
  }

  object Const {
    internal val GROUP = EventLogGroup("bazel.project.statistics", 1, "FUS", "General statistics about Bazel projects")

    internal val COUNT_TARGETS =
      GROUP.registerEvent(
        "count.targets",
        EventFields.LogarithmicInt("count_targets", "number of targets synced"),
      )
    internal val COUNT_FILES =
      GROUP.registerEvent(
        "count.files",
        EventFields.LogarithmicInt("count_files", "number of files synced"),
      )
  }
}
