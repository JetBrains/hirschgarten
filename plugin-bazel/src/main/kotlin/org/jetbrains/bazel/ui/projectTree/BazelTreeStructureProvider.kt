package org.jetbrains.bazel.ui.projectTree

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.NodeSortKey
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewSettings
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
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.impl.WorkspaceModelInternal
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.storage.CachedValue
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.platform.workspace.storage.entities
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import org.jetbrains.bazel.config.BazelPluginBundle
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelProjectDirectoriesEntity

private class BazelTreeStructureProvider : TreeStructureProvider {
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
    val newSettings = wrapViewSettings(settings)
    return when (parent) {
      is ProjectViewProjectNode -> calculateChildrenForProjectNode(project, children, newSettings)
      else -> children.removeModuleAndModuleGroupNodes()
    }
  }

  /**
   * only wrap [ProjectViewSettings]
   */
  private fun wrapViewSettings(settings: ViewSettings): ViewSettings =
    when (settings) {
      is ProjectViewSettings ->
        object : ProjectViewSettings by settings {
          override fun isAbbreviatePackageNames(): Boolean = false

          override fun isFlattenPackages(): Boolean = false

          /**
           * this is to mitigate the issue of incorrectly compacting the middle packages.
           *
           * Refer to [this issue](https://youtrack.jetbrains.com/issue/BAZEL-1879) for more info.
           *
           * TODO: remove this when the issue is addressed in IJ Platform
           */
          override fun isHideEmptyMiddlePackages(): Boolean = false

          override fun isCompactDirectories(): Boolean = settings.isCompactDirectories

          override fun isShowLibraryContents(): Boolean = settings.isShowLibraryContents

          override fun isFoldersAlwaysOnTop(): Boolean = settings.isFoldersAlwaysOnTop

          override fun isShowMembers(): Boolean = settings.isShowMembers

          override fun isStructureView(): Boolean = settings.isStructureView

          override fun isShowModules(): Boolean = settings.isShowModules

          override fun isShowScratchesAndConsoles(): Boolean = settings.isShowScratchesAndConsoles

          override fun isFlattenModules(): Boolean = settings.isFlattenModules

          override fun isShowURL(): Boolean = settings.isShowURL

          override fun isShowExcludedFiles(): Boolean = settings.isShowExcludedFiles

          override fun isShowVisibilityIcons(): Boolean = settings.isShowVisibilityIcons

          override fun getSortKey(): NodeSortKey = settings.sortKey

          override fun isUseFileNestingRules(): Boolean = settings.isUseFileNestingRules
        }

      else -> settings
    }

  private fun calculateChildrenForProjectNode(
    project: Project,
    children: Collection<AbstractTreeNode<*>>,
    settings: ViewSettings,
  ): Collection<AbstractTreeNode<*>> {
    val rootDirectory =
      PsiManager.getInstance(project).findDirectory(project.rootDir) ?: return children // should never happen

    val showExcludedDirectoriesAsSeparateNode = project.treeStructureSettings?.showExcludedDirectoriesAsSeparateNode ?: true

    val rootDirectoryNodeFilter =
      if (showExcludedDirectoriesAsSeparateNode) {
        PsiFileSystemItemFilter { item ->
          item !is PsiDirectory ||
            item.virtualFile in getDirectoriesContainingIncludedDirectories(project) ||
            !item.project.isExcluded(item.virtualFile)
        }
      } else {
        null
      }

    val rootDirectoryNode = BazelDirectoryNode(project, rootDirectory, settings, rootDirectoryNodeFilter)
    val externalLibrariesNode = children.filterIsInstance<ExternalLibrariesNode>().firstOrNull()

    val excludedDirectoriesNode =
      if (showExcludedDirectoriesAsSeparateNode) {
        ExcludedDirectoriesNode(project, rootDirectory, settings) { item ->
          if (item is PsiDirectory) {
            item.virtualFile in getDirectoriesContainingExcludedDirectories(project) ||
              item.project.isExcluded(item.virtualFile)
          } else {
            val parent = item.parent ?: return@ExcludedDirectoriesNode false
            // Only show files that aren't already shown by the root directory node
            // rootDirectoryNodeFilter returns true unconditionally if a node is not a PsiDirectory, so we check the parent here.
            rootDirectoryNodeFilter?.shouldShow(parent) == false
          }
        }
      } else {
        null
      }

    return listOfNotNull(rootDirectoryNode, externalLibrariesNode, excludedDirectoriesNode)
  }

  private fun Collection<AbstractTreeNode<*>>.removeModuleAndModuleGroupNodes(): Collection<AbstractTreeNode<*>> =
    this
      .filterNot { it is ProjectViewModuleGroupNode }
      .filterNot { it is ProjectViewModuleNode }

  private fun getDirectoriesContainingIncludedDirectories(project: Project): Set<VirtualFile> =
    (WorkspaceModel.getInstance(project) as WorkspaceModelInternal).entityStorage.cachedValue(directoriesContainingIncludedDirectoriesValue)

  private fun getDirectoriesContainingExcludedDirectories(project: Project): Set<VirtualFile> =
    (WorkspaceModel.getInstance(project) as WorkspaceModelInternal).entityStorage.cachedValue(directoriesContainingExcludedDirectoriesValue)

  private val directoriesContainingIncludedDirectoriesValue =
    CachedValue { storage ->
      storage.directoriesContainingDirectories(included = true)
    }

  private val directoriesContainingExcludedDirectoriesValue =
    CachedValue { storage ->
      storage.directoriesContainingDirectories(included = false)
    }

  private fun EntityStorage.directoriesContainingDirectories(included: Boolean): Set<VirtualFile> {
    val bazelProjectDirectories = this.bazelProjectDirectoriesEntity() ?: return emptySet()

    val roots = if (included) bazelProjectDirectories.includedRoots else bazelProjectDirectories.excludedRoots

    val result =
      roots
        .asSequence()
        .mapNotNull { it.virtualFile }
        .mapNotNull { if (it.isDirectory) it else it.parent }
        .toMutableSet()

    val toVisit = ArrayDeque(result)

    while (toVisit.isNotEmpty()) {
      val dir = toVisit.removeFirst()
      val parent = dir.parent ?: continue
      if (parent in result) continue
      result.add(parent)
      toVisit.add(parent)
    }

    return result
  }

  /**
   * This takes inspiration from [WorkspaceFileIndexDataImpl.getFileInfo] which we can't use here directly
   * because on IDEA 2025.2 we by default don't add any file sets for `includedDirectories`, just one for the root directory.
   */
  private fun Project.isExcluded(virtualFile: VirtualFile): Boolean {
    val (includedDirectories, excludedDirectories) = getIncludedAndExcludedDirectories(this)
    var current: VirtualFile? = virtualFile
    while (current != null) {
      if (current in includedDirectories) return false
      if (current in excludedDirectories) return true
      current = current.parent
    }
    return true
  }

  private fun getIncludedAndExcludedDirectories(project: Project): Pair<Set<VirtualFile>, Set<VirtualFile>> =
    (WorkspaceModel.getInstance(project) as WorkspaceModelInternal).entityStorage.cachedValue(includedAndExcludedDirectoriesValue)

  private val includedAndExcludedDirectoriesValue =
    CachedValue { storage ->
      val bazelProjectDirectories =
        storage.bazelProjectDirectoriesEntity()
          ?: return@CachedValue (emptySet<VirtualFile>() to emptySet<VirtualFile>())
      bazelProjectDirectories.includedRoots.toVirtualFileSet() to bazelProjectDirectories.excludedRoots.toVirtualFileSet()
    }

  private fun List<VirtualFileUrl>.toVirtualFileSet(): Set<VirtualFile> =
    asSequence()
      .mapNotNull { it.virtualFile }
      .toSet()

  private fun EntityStorage.bazelProjectDirectoriesEntity(): BazelProjectDirectoriesEntity? =
    entities<BazelProjectDirectoriesEntity>().firstOrNull()
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
    return directory.calculateCustomChildrenNodes(project, settings) ?: super.getChildrenImpl()
  }
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

  // Display below the project directories node
  override fun getTypeSortWeight(sortByType: Boolean): Int = super.getTypeSortWeight(sortByType) + 1
}
