package org.jetbrains.bazel.flow.sync

import com.intellij.openapi.components.serviceAsync
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.toNioPathOrNull
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
import org.jetbrains.bazel.flow.exclude.BazelSymlinkExcludeService
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.indexAdditionalFilesInDirectories
import org.jetbrains.bazel.languages.projectview.indexAllFilesInDirectories
import org.jetbrains.bazel.project.projectViewFile
import org.jetbrains.bazel.sync.ProjectSyncHook
import org.jetbrains.bazel.sync.ProjectSyncHook.ProjectSyncHookEnvironment
import org.jetbrains.bazel.sync.withSubtask
import org.jetbrains.bazel.workspace.indexAdditionalFiles.IndexAdditionalFilesContributor
import org.jetbrains.bazel.workspace.indexAdditionalFiles.ProjectViewGlobSet
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bazel.workspacemodel.entities.NonIndexableVirtualFileUrl
import kotlin.io.path.absolutePathString

private val INDEX_ADDITIONAL_FILES_DEFAULT =
  Constants.WORKSPACE_FILE_NAMES + Constants.BUILD_FILE_NAMES + Constants.MODULE_BAZEL_FILE_NAME +
  Constants.SUPPORTED_EXTENSIONS.map { extension -> "*.$extension" }


/**
 * This sync hook does three important things:
 * 1. Creates the WSM entity
 * 2. Supports [org.jetbrains.bazel.languages.projectview.language.sections.IndexAdditionalFilesInDirectoriesSection],
 *    see documentation for that class.
 * 3. Loads all non-indexable files that happen to be under `directories:` (and not excluded) into the VFS,
 *    so that "Go to file by name" is quicker, see https://youtrack.jetbrains.com/issue/IJPL-207088
 */
internal class DirectoriesSyncHook : ProjectSyncHook {
  override suspend fun onSync(environment: ProjectSyncHookEnvironment) {
    val virtualFileUrlManager = environment.project.serviceAsync<WorkspaceModel>().getVirtualFileUrlManager()

    val directoryRoots = environment.withSubtask("Collect project directories") {
      computeProjectDirectories(environment, virtualFileUrlManager)
    }

    val indexAdditionalFiles = environment.withSubtask("Collect additional files to index") {
      computeIndexAdditionalFiles(environment, virtualFileUrlManager, directoryRoots)
    }

    val indexAllFilesInIncludedRoots = environment.server.projectView.indexAllFilesInDirectories
    environment.diff.addEntity(
      BazelProjectDirectoriesEntity(
        projectRoot = environment.project.rootDir.toVirtualFileUrl(virtualFileUrlManager),
        includedRoots = directoryRoots.included,
        excludedRoots = directoryRoots.excluded,
        indexAllFilesInIncludedRoots = indexAllFilesInIncludedRoots,
        indexAdditionalFiles = indexAdditionalFiles,
        entitySource = BazelProjectEntitySource,
      )
    )
  }

  private data class DirectoryRoots(
    val included: List<NonIndexableVirtualFileUrl>,
    val excluded: List<NonIndexableVirtualFileUrl>
  )

  private suspend fun computeProjectDirectories(environment: ProjectSyncHookEnvironment, virtualFileUrlManager: VirtualFileUrlManager): DirectoryRoots {
    val directories = environment.server.workspaceDirectories(environment.snapshot.repoMapping, environment.taskId)
    val additionalExcludes = BazelSymlinkExcludeService.getInstance(environment.project).scanForBazelSymlinksToExclude(environment.project.rootDir.toNioPath())

    val includedRoots =
      directories.includedDirectories.map { virtualFileUrlManager.fromPath(it.absolutePathString()) }
    val excludedRoots =
      directories.excludedDirectories.map { virtualFileUrlManager.fromPath(it.absolutePathString()) } +
      additionalExcludes.map { it.toVirtualFileUrl(virtualFileUrlManager) }

    return DirectoryRoots(
      included = includedRoots.map { NonIndexableVirtualFileUrl(it) },
      excluded = excludedRoots.map { NonIndexableVirtualFileUrl(it) },
    )
  }


  private fun computeIndexAdditionalFiles(
    environment: ProjectSyncHookEnvironment,
    virtualFileUrlManager: VirtualFileUrlManager,
    directoryRoots: DirectoryRoots,
  ): List<NonIndexableVirtualFileUrl> {
    val project = environment.project
    val mutableEntityStorage = environment.diff

    val indexAdditionalFiles: Set<VirtualFileUrl> =
      buildSet {
        addAll(indexAdditionalFilesByName(project, environment.server.projectView, mutableEntityStorage, directoryRoots, virtualFileUrlManager))
        addAll(getProjectView(project, virtualFileUrlManager))
        addAll(getWorkspaceFiles(project, virtualFileUrlManager))

        for (contributor in IndexAdditionalFilesContributor.ep.extensionList) {
          addAll(contributor.getAdditionalFiles(project))
        }
      }

    return indexAdditionalFiles.map { NonIndexableVirtualFileUrl(it) }
  }

  private fun indexAdditionalFilesByName(
    project: Project,
    projectView: ProjectView,
    mutableEntityStorage: MutableEntityStorage,
    directoryRoots: DirectoryRoots,
    virtualFileUrlManager: VirtualFileUrlManager,
  ): List<VirtualFileUrl> {
    if (projectView.indexAllFilesInDirectories) {
      return emptyList()
    }
    val indexAdditionalFilesGlob =
      ProjectViewGlobSet(
        project.rootDir.toNioPath(),
        projectView.indexAdditionalFilesInDirectories + INDEX_ADDITIONAL_FILES_DEFAULT,
      )

    val includedRoots = directoryRoots.included.mapNotNull { it.url.virtualFile }
    val excludedRoots = directoryRoots.excluded.mapNotNullTo(hashSetOf()) { it.url.virtualFile }
    val contentRoots =
      mutableEntityStorage
        .entities<ContentRootEntity>()
        .map { it.url }
        .mapNotNullTo(hashSetOf()) { it.virtualFile }

    fun VirtualFile.isUnderContentRoot(): Boolean {
      var current: VirtualFile? = this
      while (current != null) {
        if (current in contentRoots) return true
        if (current in excludedRoots) return false
        current = current.parent
      }
      return false
    }

    val includedRootsToIterate = includedRoots.filter { !it.isUnderContentRoot() }
    val visited = hashSetOf<VirtualFile>()

    val indexAdditionalFiles = hashSetOf<VirtualFile>()

    for (includedRoot in includedRootsToIterate) {
      VfsUtilCore.visitChildrenRecursively(
        includedRoot,
        object : VirtualFileVisitor<Unit>() {
          override fun visitFileEx(file: VirtualFile): Result {
            if (file in excludedRoots || file in contentRoots) return SKIP_CHILDREN
            if (!visited.add(file)) return SKIP_CHILDREN
            if (file.isDirectory) return CONTINUE
            if (file.toNioPathOrNull()?.let { indexAdditionalFilesGlob.matches(it) } == true) {
              indexAdditionalFiles.add(file)
            }
            return CONTINUE
          }
        },
      )
    }

    return indexAdditionalFiles.map { it.toVirtualFileUrl(virtualFileUrlManager) }
  }

  private fun getProjectView(project: Project, virtualFileUrlManager: VirtualFileUrlManager): List<VirtualFileUrl> =
    listOfNotNull(project.projectViewFile.toVirtualFileUrl(virtualFileUrlManager))

  private fun getWorkspaceFiles(project: Project, virtualFileUrlManager: VirtualFileUrlManager): List<VirtualFileUrl> =
    Constants.WORKSPACE_FILE_NAMES
      .mapNotNull { name ->
        project.rootDir.findChild(name)
      }.map {
        it.toVirtualFileUrl(virtualFileUrlManager)
      }

}
