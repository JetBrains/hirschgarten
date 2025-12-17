package org.jetbrains.bazel.workspace.indexAdditionalFiles

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.entities
import com.intellij.platform.workspace.storage.impl.url.toVirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.sync.withSubtask
import org.jetbrains.bazel.target.targetUtils
import org.jetbrains.bazel.workspace.bazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.modifyBazelProjectDirectoriesEntity
import org.jetbrains.bsp.protocol.utils.extractGoBuildTarget

val INDEX_ADDITIONAL_FILES_DEFAULT =
  Constants.WORKSPACE_FILE_NAMES + Constants.BUILD_FILE_NAMES + Constants.MODULE_BAZEL_FILE_NAME +
    Constants.SUPPORTED_EXTENSIONS.map { extension -> "*.$extension" }

/**
 * This sync hook does two important things:
 * 1. Supports [org.jetbrains.bazel.languages.projectview.language.sections.IndexAdditionalFilesInDirectoriesSection],
 *    see documentation for that class.
 * 2. Loads all non-indexable files that happen to be under `directories:` (and not excluded) into the VFS,
 *    so that "Go to file by name" is quicker, see https://youtrack.jetbrains.com/issue/IJPL-207088
 */
private class IndexAdditionalFilesSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHook.ProjectSyncHookEnvironment) =
    environment.withSubtask("Collect additional files to index") {
      val project = environment.project

      val mutableEntityStorage = environment.diff.workspaceModelDiff.mutableEntityStorage
      val projectDirectoriesEntity = checkNotNull(mutableEntityStorage.bazelProjectDirectoriesEntity())
      val virtualFileUrlManager = project.serviceAsync<WorkspaceModel>().getVirtualFileUrlManager()

      val indexAdditionalFiles: Set<VirtualFileUrl> =
        buildSet {
          this += indexAdditionalFilesByName(environment, mutableEntityStorage, projectDirectoriesEntity, virtualFileUrlManager)
          getProjectView(project, virtualFileUrlManager)?.let { this += it }
          this += getWorkspaceFiles(project, virtualFileUrlManager)

          for (contributor in IndexAdditionalFilesContributor.ep.extensionList) {
            this += contributor.getAdditionalFiles(project)
          }
        }

      mutableEntityStorage.modifyBazelProjectDirectoriesEntity(projectDirectoriesEntity) {
        this.indexAdditionalFiles += indexAdditionalFiles
      }
    }

  private suspend fun indexAdditionalFilesByName(
    environment: ProjectSyncHook.ProjectSyncHookEnvironment,
    mutableEntityStorage: MutableEntityStorage,
    projectDirectoriesEntity: BazelProjectDirectoriesEntity,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<VirtualFileUrl> {
    val workspaceContext =
      query("workspace/context") {
        environment.server.workspaceContext()
      }
    if (workspaceContext.indexAllFilesInDirectories) {
      return emptyList()
    }
    val indexAdditionalFilesGlob =
      ProjectViewGlobSet(workspaceContext.indexAdditionalFilesInDirectories + INDEX_ADDITIONAL_FILES_DEFAULT)

    val includedRoots = projectDirectoriesEntity.includedRoots.mapNotNull { it.virtualFile }
    val excludedRoots = projectDirectoriesEntity.excludedRoots.mapNotNull { it.virtualFile }.toSet()
    val contentRoots =
      mutableEntityStorage
        .entities<ContentRootEntity>()
        .map { it.url }
        .mapNotNull { it.virtualFile }
        .toSet()

    fun VirtualFile.isUnderContentRoot(): Boolean {
      var current: VirtualFile? = this
      while (current != null) {
        if (this in contentRoots) return true
        if (this in excludedRoots) return false
        current = current.parent
      }
      return false
    }

    val includedRootsToIterate = includedRoots.filter { !it.isUnderContentRoot() }
    val visited = mutableSetOf<VirtualFile>()

    val indexAdditionalFiles = mutableSetOf<VirtualFile>()
    val rootDir = environment.project.rootDir

    for (includedRoot in includedRootsToIterate) {
      VfsUtilCore.visitChildrenRecursively(
        includedRoot,
        object : VirtualFileVisitor<Unit>() {
          override fun visitFileEx(file: VirtualFile): Result {
            if (file in excludedRoots || file in contentRoots) return SKIP_CHILDREN
            if (!visited.add(file)) return SKIP_CHILDREN
            if (file.isDirectory) return CONTINUE
            val relativePath = VfsUtilCore.getRelativePath(file, rootDir)
            if (relativePath != null && indexAdditionalFilesGlob.matches(relativePath)) {
              indexAdditionalFiles.add(file)
            }
            return CONTINUE
          }
        },
      )
    }

    return indexAdditionalFiles.map { it.toVirtualFileUrl(virtualFileUrlManager) }
  }

  private fun getProjectView(project: Project, virtualFileUrlManager: VirtualFileUrlManager): VirtualFileUrl? {
    return project.bazelProjectSettings.projectViewPath?.toVirtualFileUrl(virtualFileUrlManager)
  }

  private fun getWorkspaceFiles(project: Project, virtualFileUrlManager: VirtualFileUrlManager): List<VirtualFileUrl> =
    Constants.WORKSPACE_FILE_NAMES
      .mapNotNull { name ->
        project.rootDir.findChild(name)
      }.map {
        it.toVirtualFileUrl(virtualFileUrlManager)
      }
}
