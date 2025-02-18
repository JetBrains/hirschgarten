package org.jetbrains.bazel.ui.widgets.tool.window.filter

import org.jetbrains.bazel.target.TargetUtils
import org.jetbrains.bazel.workspacemodel.entities.BuildTargetInfo

public class TargetFilter(private val onFilterChange: () -> Unit) {
  public var currentFilter: FILTER = FILTER.OFF
    set(value) {
      if (field != value) {
        field = value
        onFilterChange()
      }
    }

  public fun isFilterOn(): Boolean = currentFilter != FILTER.OFF

  public fun getMatchingLoadedTargets(xd: TargetUtils): List<BuildTargetInfo> =
    xd.allTargets().mapNotNull { xd.getBuildTargetInfoForLabel(it) }.filterTargets()

  private fun List<BuildTargetInfo>.filterTargets(): List<BuildTargetInfo> = this.filter(currentFilter.predicate)

  public enum class FILTER(public val predicate: (BuildTargetInfo) -> Boolean) {
    OFF({ true }),
    CAN_RUN({ it.capabilities.canRun }),
    CAN_TEST({ it.capabilities.canTest }),
  }
}
