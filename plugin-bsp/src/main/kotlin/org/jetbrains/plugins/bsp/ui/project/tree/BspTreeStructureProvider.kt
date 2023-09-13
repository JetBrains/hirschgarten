package org.jetbrains.plugins.bsp.ui.project.tree

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ProjectViewModuleNode
import com.intellij.ide.util.treeView.AbstractTreeNode

public class BspTreeStructureProvider : TreeStructureProvider {
  override fun modify(
    parent: AbstractTreeNode<*>,
    children: MutableCollection<AbstractTreeNode<*>>,
    settings: ViewSettings,
  ): MutableCollection<AbstractTreeNode<*>> =
    children
      .removeModuleNodes()
      .toMutableList()

  // we want to get rid of all the module nodes from the project view tree;
  // in rare cases with complicated project (modules) structure IJ
  // doesn't know how to render the project tree,
  // what causes flat directory structure
  private fun MutableCollection<AbstractTreeNode<*>>.removeModuleNodes() =
    this.filterNot { it is ProjectViewModuleNode }
}
