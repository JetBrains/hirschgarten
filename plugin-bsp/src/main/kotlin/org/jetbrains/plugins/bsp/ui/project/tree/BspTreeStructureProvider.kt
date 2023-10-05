package org.jetbrains.plugins.bsp.ui.project.tree

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ExternalLibrariesNode
import com.intellij.ide.projectView.impl.nodes.ProjectViewModuleGroupNode
import com.intellij.ide.projectView.impl.nodes.ProjectViewModuleNode
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.file.PsiDirectoryImpl
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.plugins.bsp.config.rootDir

public class BspTreeStructureProvider: TreeStructureProvider {
  override fun modify(
    parent: AbstractTreeNode<*>,
    children: MutableCollection<AbstractTreeNode<*>>,
    settings: ViewSettings,
  ): MutableCollection<AbstractTreeNode<*>> =
    if (parent is ProjectViewProjectNode) createRootDirAndAddExternalLibraries(parent, children)
    else children.removeModuleAndModuleGroupsNodes()

  private fun createRootDirAndAddExternalLibraries(
    parent: ProjectViewProjectNode,
    children: MutableCollection<AbstractTreeNode<*>>,
  ): MutableCollection<AbstractTreeNode<*>> {
    val rootDirectory = PsiDirectoryImpl(PsiManagerImpl(parent.project), parent.project.rootDir)
    val rootDirectoryNode = PsiDirectoryNode(parent.project, rootDirectory, parent.settings)

    val externalLibrariesNode = children.firstIsInstanceOrNull<ExternalLibrariesNode>()

    return listOfNotNull(rootDirectoryNode, externalLibrariesNode).toMutableList()
  }

  // we want to get rid of all the module (group) nodes from the project view tree;
  // in rare cases with complicated project (modules) structure IJ
  // doesn't know how to render the project tree,
  // what causes flat directory structure
  private fun MutableCollection<AbstractTreeNode<*>>.removeModuleAndModuleGroupsNodes():
    MutableCollection<AbstractTreeNode<*>> =
    this
      .filterNot { it is ProjectViewModuleGroupNode }
      .filterNot { it is ProjectViewModuleNode }
      .toMutableList()
}
