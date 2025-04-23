package org.jetbrains.bazel.ui.widgets.tool.window.filter

import org.jetbrains.bazel.commons.RuleType
import org.jetbrains.bazel.target.TargetUtils
import org.jetbrains.bsp.protocol.BuildTarget

public class TargetFilter(private val onFilterChange: () -> Unit) {
  public var currentFilter: FILTER = FILTER.OFF
    set(value) {
      if (field != value) {
        field = value
        onFilterChange()
      }
    }

  public fun isFilterOn(): Boolean = currentFilter != FILTER.OFF

  public fun getMatchingLoadedTargets(utils: TargetUtils): List<BuildTarget> =
    utils.allTargets().mapNotNull { utils.getBuildTargetForLabel(it) }.filterTargets()

  private fun List<BuildTarget>.filterTargets(): List<BuildTarget> = this.filter(currentFilter.predicate)

  public enum class FILTER(public val predicate: (BuildTarget) -> Boolean) {
    OFF({ true }),
    CAN_RUN({ it.kind.ruleType == RuleType.BINARY }),
    CAN_TEST({ it.kind.ruleType == RuleType.TEST }),
  }
}
