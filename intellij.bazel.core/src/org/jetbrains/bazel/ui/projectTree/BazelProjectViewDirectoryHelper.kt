package org.jetbrains.bazel.ui.projectTree

import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper
import com.intellij.ide.projectView.impl.nodes.PsiFileSystemItemFilter
import com.intellij.java.workspace.entities.javaSourceRoots
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.findEntitiesByVirtualFile
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFileSystemItem
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.workspace.packageMarker.concatenatePackages
import org.jetbrains.bazel.workspacemodel.entities.PackageMarkerEntity

@ApiStatus.Internal
class BazelProjectViewDirectoryHelper(project: Project) : ProjectViewDirectoryHelper(project) {
  private val fileIndex = ProjectFileIndex.getInstance(project)
  private val delegate = getInstance(project)

  fun getPackageName(directory: PsiDirectory): String? {
    val file = directory.virtualFile
    val root = fileIndex.getSourceRootForFile(file) ?: return null
    val entities = getRootEntities(root).toList()
    val marker = entities.filterIsInstance<PackageMarkerEntity>().firstOrNull()
    if (marker != null) {
      return marker.packagePrefix
    }

    val properties =
      entities
        .filterIsInstance<SourceRootEntity>()
        .firstNotNullOfOrNull { it.javaSourceRoots.firstOrNull() }
      ?: return null
    val relativeName = VfsUtilCore.getRelativePath(file, root, '.') ?: return null
    return concatenatePackages(properties.packagePrefix, relativeName)
  }

  fun isSourceRoot(directory: PsiDirectory): Boolean =
    getRootEntities(directory.virtualFile).any { it is SourceRootEntity }

  private fun getRootEntities(file: VirtualFile) = WorkspaceModel.getInstance(project).let { workspaceModel ->
    workspaceModel.currentSnapshot.getVirtualFileUrlIndex().findEntitiesByVirtualFile(file, workspaceModel.getVirtualFileUrlManager())
  }

  override fun skipDirectory(directory: PsiDirectory): Boolean =
    getPackageName(directory) == null && delegate.skipDirectory(directory)

  override fun isEmptyMiddleDirectory(directory: PsiDirectory, strictlyEmpty: Boolean, filter: PsiFileSystemItemFilter?): Boolean {
    if (getPackageName(directory) == null) return delegate.isEmptyMiddleDirectory(directory, strictlyEmpty, filter)
    var subdirectories = 0
    for (child in directory.children) {
      if (child !is PsiFileSystemItem || FileTypeRegistry.getInstance().isFileIgnored(child.virtualFile) ||
          filter?.shouldShow(child) == false) {
        continue
      }
      if (child !is PsiDirectory || skipDirectory(child)) return false
      subdirectories++
      if (strictlyEmpty && subdirectories > 1) return false
    }
    return subdirectories > 0
  }
}
