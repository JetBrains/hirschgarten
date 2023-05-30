package org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter

import ch.epfl.scala.bsp4j.BuildTarget
import org.jetbrains.magicmetamodel.MagicMetaModel

public class TargetFilter(
  private val onFilterChange: () -> Unit
) {
  public var currentFilter: FILTER = FILTER.OFF
    set(value) {
      if (field != value) {
        field = value
        onFilterChange()
      }
    }

  public fun isFilterOn(): Boolean = currentFilter != FILTER.OFF

  public fun getMatchingLoadedTargets(magicMetaModel: MagicMetaModel): List<BuildTarget> =
    magicMetaModel.getAllLoadedTargets().filterTargets()

  public fun getMatchingNotLoadedTargets(magicMetaModel: MagicMetaModel): List<BuildTarget> =
    magicMetaModel.getAllNotLoadedTargets().filterTargets()

  private fun List<BuildTarget>.filterTargets(): List<BuildTarget> =
    this.filter(currentFilter.predicate)

  public enum class FILTER(public val predicate: (BuildTarget) -> Boolean) {
    OFF({ true }),
    CAN_RUN({ it.capabilities.canRun }),
    CAN_TEST({ it.capabilities.canTest })
  }
}
