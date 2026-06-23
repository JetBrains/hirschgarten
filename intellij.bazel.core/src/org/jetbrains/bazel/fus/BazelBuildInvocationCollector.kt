package org.jetbrains.bazel.fus

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.project.Project

internal object BazelBuildInvocationCollector : CounterUsagesCollector() {
  private val GROUP = EventLogGroup("bazel.build.invocations", 2, "FUS")
  private val TARGET_COUNT = EventFields.RoundedInt("target_count")
  private val STARTED = GROUP.registerEvent("started", TARGET_COUNT)
  private val FINISHED = GROUP.registerEvent("finished")

  override fun getGroup(): EventLogGroup = GROUP

  fun logStarted(project: Project, targetCount: Int) {
    STARTED.log(project, targetCount)
  }

  fun logFinished(project: Project) {
    FINISHED.log(project)
  }
}
