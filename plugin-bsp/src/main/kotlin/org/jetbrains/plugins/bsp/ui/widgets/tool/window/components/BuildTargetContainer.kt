package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

import ch.epfl.scala.bsp4j.BuildTarget
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.CopyTargetIdAction
import java.awt.event.MouseListener

/**
 * Represents a container, which contains build targets and shows them in its UI representation
 */
public interface BuildTargetContainer {
  /**
   * Action responsible for copying target IDs inside this container
   */
  public val copyTargetIdAction: CopyTargetIdAction

  /**
   * Returns `true` if this container contains no targets and `false` otherwise
   */
  public fun isEmpty(): Boolean

  /**
   * Adds a mouse listener to this container's UI representation
   *
   * @param listenerBuilder mouse listener builder, which will be provided with this container as its argument
   */
  public fun addMouseListener(listenerBuilder: (BuildTargetContainer) -> MouseListener)

  /**
   * Obtains the selected build target, if any
   *
   * @return selected build target, or `null` if nothing is selected
   */
  public fun getSelectedBuildTarget(): BuildTarget?

  /**
   * Creates a new instance of this container. The new instance will have similar mouse listeners
   *
   * @param newTargets collection of build targets the new container will contain
   * @return the newly created container
   */
  public fun createNewWithTargets(newTargets: Collection<BuildTarget>): BuildTargetContainer
}
