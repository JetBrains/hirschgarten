package org.jetbrains.bazel.clion.workspace

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration
import com.jetbrains.cidr.lang.workspace.OCResolveConfigurationChooser
import com.jetbrains.cidr.lang.workspace.OCResolveConfigurationSlice
import com.jetbrains.cidr.lang.workspace.OCWorkspace
import org.jetbrains.bazel.sync.environment.projectCtx

/** Selects a configuration for files that are not a source of any configuration, e.g. newly created files. */
internal class CcDirectoryBasedChooser : OCResolveConfigurationChooser {

  override fun selectPreselectedResolveConfiguration(
    project: Project,
    slice: OCResolveConfigurationSlice,
    file: VirtualFile?,
  ): OCResolveConfiguration? {
    // a specific slice means the file is known to the workspace, leave the choice to the other choosers
    if (file == null || slice !is OCResolveConfigurationSlice.All || !project.projectCtx.isBazelProject) return null

    val projectRoot = project.projectCtx.projectRootDir ?: return null
    val workspace = OCWorkspace.getInstance(project)

    var directory = file.parent
    while (directory != null && VfsUtilCore.isAncestor(projectRoot, directory, /* strict = */ false)) {
      findConfigurationInDirectory(workspace, directory)?.let { return it }
      directory = directory.parent
    }

    return null
  }
}

/** Returns the first configuration of the sources in [directory]. */
private fun findConfigurationInDirectory(workspace: OCWorkspace, directory: VirtualFile): OCResolveConfiguration? {
  for (child in directory.children) {
    ProgressManager.checkCanceled()
    if (child.isDirectory) continue

    for (configuration in workspace.getConfigurationsForFile(child)) {
      return configuration
    }
  }

  return null
}
