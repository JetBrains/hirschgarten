package org.jetbrains.plugins.bsp.ui.projectTree

import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ExternalLibrariesNode
import com.intellij.ide.projectView.impl.nodes.ProjectViewModuleGroupNode
import com.intellij.ide.projectView.impl.nodes.ProjectViewModuleNode
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.plugins.bsp.config.isBspProject
import org.jetbrains.plugins.bsp.config.isBspProjectLoaded
import org.jetbrains.plugins.bsp.config.openedTimesSinceLastStartupResync
import org.jetbrains.plugins.bsp.config.rootDir

class BspTreeStructureProvider : TreeStructureProvider {
  // We want to get rid of all the module (group) nodes from the project view tree;
  // in rare cases with complicated project (modules) structure IJ
  // doesn't know how to render the project tree,
  // what causes flat directory structure.
  // Instead we use the root directory node as the root of the project view tree and ignore modules altogether.
  // This results in displaying the folder structure of the project as it is on the file system.
  override fun modify(
    parent: AbstractTreeNode<*>,
    children: Collection<AbstractTreeNode<*>>,
    settings: ViewSettings,
  ): Collection<AbstractTreeNode<*>> {
    val project = parent.project ?: return children
    if (!project.isBspProject) return children

    return when (parent) {
      is ProjectViewProjectNode -> calculateChildrenForProjectNode(project, children, settings)
      else -> children.removeModuleAndModuleGroupNodes()
    }
  }

  private fun calculateChildrenForProjectNode(
    project: Project,
    children: Collection<AbstractTreeNode<*>>,
    settings: ViewSettings,
  ): Collection<AbstractTreeNode<*>> {
    val rootDirectory =
      project.service<PsiManager>().findDirectory(project.rootDir) ?: return children // should never happen
    val rootDirectoryNode = BspDirectoryNode(project, rootDirectory, settings)
    val externalLibrariesNode = children.firstIsInstanceOrNull<ExternalLibrariesNode>()
    return listOfNotNull(rootDirectoryNode, externalLibrariesNode)
  }

  private fun Collection<AbstractTreeNode<*>>.removeModuleAndModuleGroupNodes(): Collection<AbstractTreeNode<*>> =
    this
      .filterNot { it is ProjectViewModuleGroupNode }
      .filterNot { it is ProjectViewModuleNode }
}

/**
 * Custom PsiDirectoryNode for BSP projects to show the project view with full contents during initial sync.
 */
class BspDirectoryNode(
  project: Project,
  directory: PsiDirectory,
  viewSettings: ViewSettings?,
) : PsiDirectoryNode(project, directory, viewSettings) {
  private val lazyChildren: Collection<AbstractTreeNode<*>> by lazy {
    directory.calculateCustomChildrenNodes(project, settings)
  }

  private fun PsiDirectory.calculateCustomChildrenNodes(project: Project, settings: ViewSettings?): Collection<AbstractTreeNode<*>> =
    if (project.shouldNotCalculateCustomNodes()) {
      emptyList()
    } else {
      children.mapNotNull { psiChild ->
        when (psiChild) {
          is PsiDirectory -> BspDirectoryNode(project, psiChild, settings)
          is PsiFile -> PsiFileNode(project, psiChild, settings)
          else -> null
        }
      }
    }

  /**
   * the custom nodes should only be created on broken project state;
   * this function provides a heuristic to guess that the project state is not broken, i.e.,
   * - when just successfully finish the startup resync, OR
   * - when the BSP project is successfully loaded
   */
  private fun Project.shouldNotCalculateCustomNodes() = openedTimesSinceLastStartupResync == 1 || isBspProjectLoaded

  override fun getChildrenImpl(): Collection<AbstractTreeNode<*>?>? =
    if (lazyChildren.isNotEmpty()) {
      lazyChildren
    } else {
      super.getChildrenImpl()
    }
}
