package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.cargo.features.extension

import java.awt.event.MouseListener

/**
 * Represents a container, which contains features and shows them in its UI representation
 */
public interface FeaturesContainer {
  /**
   * Returns `true` if this container contains no features and `false` otherwise
   */
  public fun isEmpty(): Boolean

  /**
   * Adds a mouse listener to this container's UI representation
   *
   * @param mouseListener mouse listener builder, which will be provided with this container as its argument
   */
  public fun addMouseListener(mouseListener: (FeaturesContainer) -> MouseListener)

  /**
   * Checks current selection and if it is valid,
   * handles toggling selected feature and all corresponding to it features
   */
  public fun toggleFeatureIfSelected()
}
