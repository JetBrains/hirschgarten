package org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils

import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetId
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.BuildTargetInfo
import org.jetbrains.plugins.bsp.magicmetamodel.impl.workspacemodel.ModuleCapabilities

/** Holds *something* for three possible target states - loaded, unloaded and invalid */
public data class Tristate<T>(
  val loaded: T,
  val unloaded: T,
  val invalid: T,
) {
  public fun <R> map(transform: (T) -> R): Tristate<R> =
    Tristate(transform(loaded), transform(unloaded), transform(invalid))

  public fun reduce(operation: (T, T) -> T): T =
    operation(operation(loaded, unloaded), invalid)

  // This could be a Tristate<BuildTargetInfo>, but for invalid targets it would unnecessarily use more memory
  public data class Targets(
    val loaded: List<BuildTargetInfo> = emptyList(),
    val unloaded: List<BuildTargetInfo> = emptyList(),
    val invalid: List<BuildTargetId> = emptyList(),
  ) {
    public val size: Int =
      loaded.size + unloaded.size + invalid.size

    public fun isEmpty(): Boolean =
      loaded.isEmpty() && unloaded.isEmpty() && invalid.isEmpty()

    public fun filterByIds(predicate: (String) -> Boolean): Targets = Targets(
      loaded = loaded.filter { predicate(it.id) },
      unloaded = unloaded.filter { predicate(it.id) },
      invalid = invalid.filter { predicate(it) }
    )

    public fun filterByCapabilities(predicate: (ModuleCapabilities) -> Boolean): Targets {
      val noCapabilities = ModuleCapabilities(
        canRun = false,
        canTest = false,
        canCompile = false,
        canDebug = false,
      )
      return Targets(
        loaded = loaded.filter { predicate(it.capabilities) },
        unloaded = unloaded.filter { predicate(it.capabilities) },
        invalid = if (predicate(noCapabilities)) invalid else emptyList(),
      )
    }

    public companion object {
      public val EMPTY: Targets = Targets(emptyList(), emptyList(), emptyList())
    }
  }
}
