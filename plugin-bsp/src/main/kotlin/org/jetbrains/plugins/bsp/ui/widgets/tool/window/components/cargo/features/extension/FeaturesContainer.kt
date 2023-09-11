package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components.cargo.features.extension

import java.awt.event.MouseListener


/**
 * Represents a container, which contains build targets and shows them in its UI representation
 */
public interface FeaturesContainer {
    /**
     * Returns `true` if this container contains no targets and `false` otherwise
     */
    public fun isEmpty(): Boolean

    /**
     * Adds a mouse listener to this container's UI representation
     *
     * @param listenerBuilder mouse listener builder, which will be provided with this container as its argument
     */
    public fun addMouseListener(listenerBuilder: (FeaturesContainer) -> MouseListener)

    /**
     * Obtains the selected build target, if any
     */
    public fun toggleFeatureIfSelected()
}
