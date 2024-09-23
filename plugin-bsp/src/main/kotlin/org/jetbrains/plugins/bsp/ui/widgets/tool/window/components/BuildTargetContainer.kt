package org.jetbrains.plugins.bsp.ui.widgets.tool.window.components

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.ui.PopupHandler
import org.jetbrains.plugins.bsp.ui.widgets.tool.window.actions.CopyTargetIdAction
import org.jetbrains.plugins.bsp.workspacemodel.entities.BuildTargetInfo
import javax.swing.JComponent

/**
 * Represents a container, which contains build targets and shows them in its UI representation
 */
interface BuildTargetContainer {
  /**
   * Action responsible for copying target IDs inside this container
   */
  val copyTargetIdAction: CopyTargetIdAction

  /**
   * Returns `true` if this container contains no targets and `false` otherwise
   */
  fun isEmpty(): Boolean

  /**
   * @return the component representing this container
   */
  fun getComponent(): JComponent

  /**
   * Registers a popup handler and adds it as a mouse listener to this container's UI representation
   *
   * @param popupHandlerBuilder popup handler builder, which will be provided with this container as its argument
   */
  fun registerPopupHandler(popupHandlerBuilder: (BuildTargetContainer) -> PopupHandler)

  /**
   * Obtains the selected build target, if any
   *
   * @return selected build target, or `null` if nothing is selected
   */
  fun getSelectedBuildTarget(): BuildTargetInfo?

  /**
   * Selects the topmost displayed target (or directory, in case of a tree) and gives focus to this container's component
   */
  fun selectTopTargetAndFocus()

  /**
   * Creates a new instance of this container. The new instance will have similar mouse listeners
   *
   * @param newTargets collection of build targets the new container will contain
   * @return the newly created container
   */
  fun createNewWithTargets(newTargets: Collection<BuildTargetInfo>): BuildTargetContainer

  /**
   * Returns actions available for a target.
   *
   * @param project this project
   * @param buildTargetInfo information about the target
   */
  fun getTargetActions(project: Project, buildTargetInfo: BuildTargetInfo): List<AnAction>
}
