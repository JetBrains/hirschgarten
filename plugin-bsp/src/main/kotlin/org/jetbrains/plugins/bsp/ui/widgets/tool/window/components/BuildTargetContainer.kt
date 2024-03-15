package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.CopyTargetIdAction
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.TargetNode
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.utils.Tristate
import java.awt.event.MouseListener
import javax.swing.JComponent

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
   * Obtains the selected directory or build target node, if any
   *
   * @return selected node, or `null` if nothing is selected
   */
  public fun getSelectedNode(): TargetNode?

  /**
   * Creates a new instance of this container. The new instance will have similar mouse listeners
   *
   * @param newTargets collection of build targets the new container will contain
   * @return the newly created container
   */
  public fun createNewWithTargets(newTargets: Tristate.Targets): BuildTargetContainer

  /**
   * @return the component associated with this build target container
   */
  public fun getComponent(): JComponent
}
