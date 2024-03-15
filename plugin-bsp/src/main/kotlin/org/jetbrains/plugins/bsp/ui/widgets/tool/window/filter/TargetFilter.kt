package org.jetbrains.plugins.bsp.ui.widgets.tool.window.filter

import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.Tristate.Targets

public class TargetFilter(
  private val onFilterChange: () -> Unit,
) {
  public var currentCapabilityFilter: ByCapability = ByCapability.OFF
    private set

  public var currentStateFilter: ByState = ByState.OFF
    private set

  public fun updateFilter(newFilter: FilterType) {
    when (newFilter) {
      is ByCapability -> currentCapabilityFilter = newFilter
      is ByState -> currentStateFilter = newFilter
    }
    onFilterChange()
  }

  public fun filterTargets(targets: Targets): Targets =
    targets
      .filterByCapabilities(currentCapabilityFilter)
      .filterByState(currentStateFilter)

  private fun Targets.filterByCapabilities(capabilityFilter: ByCapability) =
    when (capabilityFilter) {
      ByCapability.OFF -> this
      ByCapability.CAN_RUN -> this.filterByCapabilities { it.canRun }
      ByCapability.CAN_TEST -> this.filterByCapabilities { it.canTest }
    }

  private fun Targets.filterByState(stateFilter: ByState) =
    when (stateFilter) {
      ByState.OFF -> this
      ByState.LOADED -> Targets(loaded = this.loaded)
      ByState.UNLOADED -> Targets(unloaded = this.unloaded)
      ByState.INVALID -> Targets(invalid = this.invalid)
    }

  public fun clearFilters() {
    currentCapabilityFilter = ByCapability.OFF
    currentStateFilter = ByState.OFF
    onFilterChange()
  }

  public fun areFiltersEnabled(): Boolean =
    currentCapabilityFilter != ByCapability.OFF || currentStateFilter != ByState.OFF

  public interface FilterType

  public enum class ByCapability : FilterType {
    OFF,
    CAN_RUN,
    CAN_TEST,
  }

  public enum class ByState : FilterType {
    OFF,
    LOADED,
    UNLOADED,
    INVALID,
  }
}
