package org.jetbrains.bazel.flow.sync

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.backend.workspace.virtualFile
import com.intellij.platform.workspace.jps.entities.ContentRootEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.entities
import com.intellij.platform.workspace.storage.url.VirtualFileUrl
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.modifyBazelProjectDirectoriesEntity
import org.jetbrains.bazel.settings.bazel.bazelProjectSettings
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.projectStructure.workspaceModel.workspaceModelDiff
import org.jetbrains.bazel.sync.task.query
import org.jetbrains.bazel.sync.withSubtask
import org.jetbrains.bazel.workspace.bazelProjectDirectoriesEntity

private val INDEX_ADDITIONAL_FILES_DEFAULT =
  Constants.WORKSPACE_FILE_NAMES + Constants.BUILD_FILE_NAMES + Constants.MODULE_BAZEL_FILE_NAME +
    "*.${Constants.PROJECT_VIEW_FILE_EXTENSION}"

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
    if (workspaceContext.indexAllFilesInDirectories.value) {
      return emptyList()
    }
    val acceptedNames = workspaceContext.indexAdditionalFilesInDirectoriesSpec.values.toSet() + INDEX_ADDITIONAL_FILES_DEFAULT

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

    for (includedRoot in includedRootsToIterate) {
      VfsUtilCore.visitChildrenRecursively(
        includedRoot,
        object : VirtualFileVisitor<Unit>() {
          override fun visitFileEx(file: VirtualFile): Result {
            if (file in excludedRoots || file in contentRoots) return SKIP_CHILDREN
            if (!visited.add(file)) return SKIP_CHILDREN
            if (file.isDirectory) return CONTINUE
            // TODO: use https://github.com/bazelbuild/intellij/blob/36a45506024a44472bde002171d51dda473ea68e/base/src/com/google/idea/blaze/base/projectview/section/Glob.java#L89C17-L89C39
            if (file.name in acceptedNames || "*.${file.extension.orEmpty()}" in acceptedNames) {
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
    val projectView =
      project.bazelProjectSettings.projectViewPath?.let {
        LocalFileSystem.getInstance().findFileByNioFile(it)
      }
    return projectView?.toVirtualFileUrl(virtualFileUrlManager)
  }

  private fun getWorkspaceFiles(project: Project, virtualFileUrlManager: VirtualFileUrlManager): List<VirtualFileUrl> =
    Constants.WORKSPACE_FILE_NAMES
      .mapNotNull { name ->
        project.rootDir.findChild(name)
      }.map {
        it.toVirtualFileUrl(virtualFileUrlManager)
      }
}
