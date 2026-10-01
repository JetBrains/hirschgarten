package org.jetbrains.bazel.ui.projectTree

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.TreeStructureProvider
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ExternalLibrariesNode
import com.intellij.ide.projectView.impl.nodes.ProjectViewModuleGroupNode
import com.intellij.ide.projectView.impl.nodes.ProjectViewModuleNode
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.projectView.impl.nodes.PsiFileSystemItemFilter
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.util.containers.addIfNotNull
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.ui.projectTree.BazelTreeNodeType.EXCLUDED
import org.jetbrains.bazel.ui.projectTree.BazelTreeNodeType.ROOT
import org.jetbrains.bazel.ui.projectTree.BazelTreeNodeType.UNIMPORTED

@ApiStatus.Internal
class BazelTreeStructureProvider : TreeStructureProvider {
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
    if (!project.isBazelProject) return children
    return when (parent) {
      is ProjectViewProjectNode -> calculateChildrenForProjectNode(project, children, settings)
      else -> children.removeModuleAndModuleGroupNodes().map {
        if (it is PsiDirectoryNode) BazelDirectoryNode(project, it.value, settings, it.filter)
        else it
      }
    }
  }

  private fun calculateChildrenForProjectNode(
    project: Project,
    children: Collection<AbstractTreeNode<*>>,
    settings: ViewSettings,
  ): Collection<AbstractTreeNode<*>> {
    val rootDirectory =
      PsiManager.getInstance(project).findDirectory(project.rootDir) ?: return children // should never happen
    val bazelTreeStructureController = BazelTreeStructureController.getInstance(project)
    return buildList {
      addIfNotNull(children.filterIsInstance<ExternalLibrariesNode>().firstOrNull())

      if (bazelTreeStructureController.shouldShowNode(ROOT)) {
        add(BazelDirectoryNode(project, rootDirectory, settings) { item ->
          bazelTreeStructureController.shouldShowUnderTreeNode(item.virtualFile, ROOT)
        })
      }

      if (bazelTreeStructureController.shouldShowNode(EXCLUDED)) {
        add(ExcludedDirectoriesNode(project, rootDirectory, settings) { item ->
          bazelTreeStructureController.shouldShowUnderTreeNode(item.virtualFile, EXCLUDED)
        })
      }

      if (bazelTreeStructureController.shouldShowNode(UNIMPORTED)) {
        add(UnimportedDirectoriesNode(project, rootDirectory, settings) { item ->
          bazelTreeStructureController.shouldShowUnderTreeNode(item.virtualFile, UNIMPORTED)
        })
      }
    }
  }

  private fun Collection<AbstractTreeNode<*>>.removeModuleAndModuleGroupNodes(): Collection<AbstractTreeNode<*>> =
    this
      .filterNot { it is ProjectViewModuleGroupNode }
      .filterNot { it is ProjectViewModuleNode }
}

/**
 * Custom PsiDirectoryNode for Bazel projects to show the project view with full contents during initial sync.
 */
private class BazelDirectoryNode(
  project: Project,
  directory: PsiDirectory,
  viewSettings: ViewSettings?,
  private val filter: PsiFileSystemItemFilter? = null,
) : PsiDirectoryNode(project, directory, viewSettings, filter) {

  override fun shouldShowModuleName(): Boolean = false

  override fun updateImpl(data: PresentationData) {
    super.updateImpl(data)
    if (!settings.isFlattenPackages && settings.isHideEmptyMiddlePackages) {
      val parentDirectory = parent?.value as? PsiDirectory ?: return
      val name = VfsUtilCore.getRelativePath(value.virtualFile, parentDirectory.virtualFile, '/') ?: return
      data.clearText()
      data.presentableText = name
    }
  }

  private fun PsiDirectory.calculateCustomChildrenNodes(project: Project, settings: ViewSettings?): Collection<AbstractTreeNode<*>>? =
    if (project.shouldNotCalculateCustomNodes()) {
      null
    } else {
      children.mapNotNull { psiChild ->
        if (psiChild is PsiFileSystemItem && filter?.shouldShow(psiChild) == false) return@mapNotNull null

        when (psiChild) {
          is PsiDirectory -> BazelDirectoryNode(project, psiChild, settings)
          is PsiFile -> PsiFileNode(project, psiChild, settings)
          else -> null
        }
      }
    }

  /**
   * The project view tree relies on the existence of the "fake module".
   * See https://youtrack.jetbrains.com/issue/IJPL-15946/Platform-solution-for-the-initial-state-of-the-project-model-on-the-first-open
   * However, we remove the fake module in `CounterPlatformProjectConfigurator`.
   * So while the project is syncing, we show our custom tree, so that the user can at least see the project files and directories.
   */
  private fun Project.shouldNotCalculateCustomNodes() = ModuleManager.getInstance(this).modules.isNotEmpty()

  override fun getChildrenImpl(): Collection<AbstractTreeNode<*>?>? {
    val virtualFile = virtualFile ?: return null
    val directory = PsiManager.getInstance(project).findDirectory(virtualFile) ?: return null
    directory.calculateCustomChildrenNodes(project, settings)?.let { return it }

    val helper = BazelProjectViewDirectoryHelper(project)
    val children = helper.getDirectoryChildren(directory, settings, true, filter)
    if (!settings.isFlattenPackages) {
      if (!settings.isHideEmptyMiddlePackages) {
        return children
      }
      if (!helper.skipDirectory(directory)) {
        return children.map { child ->
          if (child !is PsiDirectoryNode) return@map child
          var childDirectory = child.value
          while (!helper.isSourceRoot(childDirectory) && helper.isEmptyMiddleDirectory(childDirectory, true, filter)) {
            ProgressManager.checkCanceled()
            childDirectory = childDirectory.subdirectories.single {
              !FileTypeRegistry.getInstance().isFileIgnored(it.virtualFile) && filter?.shouldShow(it) != false
            }
          }
          BazelDirectoryNode(project, childDirectory, settings, filter)
        }
      }
      // outside of source roots, directories don't correspond to packages, so collapse middle directories by the structure only
      return children.map { child ->
        if (child !is PsiDirectoryNode) return@map child
        var childDirectory = child.value
        while (!helper.isSourceRoot(childDirectory)) {
          ProgressManager.checkCanceled()
          childDirectory = helper.findCollapsibleSubdirectory(childDirectory, filter) ?: break
        }
        BazelDirectoryNode(project, childDirectory, settings, filter)
      }
    }

    if (helper.skipDirectory(directory)) {
      return children
    }
    val parentDirectory = directory.parentDirectory
    if (parentDirectory != null && !helper.skipDirectory(parentDirectory)) {
      return children
    }

    val result = children.filterTo(mutableListOf()) { it !is PsiDirectoryNode || helper.skipDirectory(it.value) }
    val directories = ArrayDeque<PsiDirectory>()
    directories.addAll(directory.subdirectories)
    while (directories.isNotEmpty()) {
      ProgressManager.checkCanceled()
      val subdirectory = directories.removeFirst()
      if (helper.skipDirectory(subdirectory) || filter?.shouldShow(subdirectory) == false) {
        continue
      } else if (!settings.isHideEmptyMiddlePackages || !helper.isEmptyMiddleDirectory(subdirectory, false, filter)) {
        result.add(BazelDirectoryNode(project, subdirectory, settings, filter))
      }
      directories.addAll(subdirectory.subdirectories)
    }
    return result
  }

  override fun getTypeSortWeight(sortByType: Boolean): Int = BazelTreeStructureOrder.ROOT.weight
}

private class UnimportedDirectoriesNode(
  project: Project,
  directory: PsiDirectory,
  viewSettings: ViewSettings?,
  filter: PsiFileSystemItemFilter,
) : PsiDirectoryNode(project, directory, viewSettings, filter) {
  override fun isAlwaysShowPlus(): Boolean = true

  override fun update(presentation: PresentationData) {
    presentation.presentableText = BazelPluginBundle.message("widget.project.tree.unimported.directories")
    presentation.tooltip = BazelPluginBundle.message("widget.project.tree.unimported.directories.tooltip")
    presentation.setIcon(AllIcons.Nodes.Workspace)
  }

  override fun getTypeSortWeight(sortByType: Boolean): Int = BazelTreeStructureOrder.UNIMPORTED.weight
}

private class ExcludedDirectoriesNode(
  project: Project,
  directory: PsiDirectory,
  viewSettings: ViewSettings?,
  filter: PsiFileSystemItemFilter,
) : PsiDirectoryNode(project, directory, viewSettings, filter) {
  override fun isAlwaysShowPlus(): Boolean = true

  override fun isIncludedInExpandAll(): Boolean = false

  override fun update(presentation: PresentationData) {
    presentation.presentableText = BazelPluginBundle.message("widget.project.tree.excluded.directories")
    presentation.setIcon(AllIcons.Modules.ExcludeRoot)
    // Show in gray even if the project root dir is excluded (for better visibility)
    presentation.background = null
  }

  override fun getTypeSortWeight(sortByType: Boolean): Int = BazelTreeStructureOrder.EXCLUDED.weight
}

private enum class BazelTreeStructureOrder(val weight: Int) {
  ROOT(1),
  UNIMPORTED(2),
  EXCLUDED(3),
}
