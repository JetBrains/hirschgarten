@file:Suppress("UnstableApiUsage")

package org.jetbrains.plugins.bsp.fus

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.project.Project

class BazelToolwindowUsagesCollector : CounterUsagesCollector() {
  override fun getGroup(): EventLogGroup = GROUP

  /**
   * Recording a tool window activation event with two fields.
   */
  companion object {
    private val GROUP: EventLogGroup = EventLogGroup("bsp.toolwindow", 0, "FUS", "BSP tool window action usages")
    private val ACTIVATED = GROUP.registerEvent("activated", "focused the BSP tool window")

    @JvmStatic
    fun logActivated(project: Project) {
      ACTIVATED.log(project)
    }
  }
}
