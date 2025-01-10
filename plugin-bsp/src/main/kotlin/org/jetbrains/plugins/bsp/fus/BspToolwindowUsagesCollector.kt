package org.jetbrains.plugins.bsp.fus

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.project.Project

class BspToolwindowUsagesCollector : CounterUsagesCollector() {

  override fun getGroup(): EventLogGroup? {
    return GROUP
  }

  /**
   * Recording a tool window activation event with two fields.
   */
  companion object {
    private val GROUP: EventLogGroup = EventLogGroup("bsp.toolwindow", 0)
    private val ACTIVATED = GROUP.registerEvent("activated", "focused the BSP tool window")

    @JvmStatic
    fun logActivated(project: Project) {
      ACTIVATED.log(project)
    }
  }
}
