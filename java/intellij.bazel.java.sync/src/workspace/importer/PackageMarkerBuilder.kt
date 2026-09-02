package org.jetbrains.bazel.workspace.importer

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.Project.DIRECTORY_STORE_FOLDER
import com.intellij.openapi.vfs.VFileProperty
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualFileOrDirectory
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.platform.backend.workspace.toVirtualFileUrl
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.modifyModuleEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.entities
import com.intellij.platform.workspace.storage.url.VirtualFileUrlManager
import com.intellij.workspaceModel.ide.toPath
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.languages.projectview.ProjectView
import org.jetbrains.bazel.languages.projectview.dotIdeaDirectoryLocation
import org.jetbrains.bazel.utils.findVirtualFile
import org.jetbrains.bazel.workspace.packageMarker.concatenatePackages
import org.jetbrains.bazel.workspacemodel.entities.BazelDummyEntitySource
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity
import org.jetbrains.bazel.workspacemodel.entities.PackageMarkerEntity
import org.jetbrains.bazel.workspacemodel.entities.PackageMarkerEntityBuilder
import org.jetbrains.bazel.workspacemodel.entities.packageMarkerEntities
import java.nio.file.Path

/**
 * Adds [PackageMarkerEntity] instances for dummy modules so the workspace model can attribute
 * orphan source directories (not covered by a real module) to a package prefix.
 *
 * Shared mutable state tracks directories already covered by non-dummy modules so dummy markers
 * skip them; therefore one [PackageMarkerBuilder] should be constructed per import pass with
 * the full list of non-dummy source roots up front.
 *
 * The directory tree is read through the VFS. A symlink, an excluded directory, a `.idea` directory,
 * and a directory that already has a marker prune their whole subtree.
 */
// RC: replaces `PackageMarkerEntityUpdater`; `alreadyVisitedDirectories` matches the old `alreadyVisitedDirectories` seed
@ApiStatus.Internal
class PackageMarkerBuilder(
  alreadyCoveredDirectories: Set<Path>,
  private val excludedDirectories: Set<Path>,
) {
  private val alreadyVisitedDirectories: MutableSet<Path> = alreadyCoveredDirectories.toMutableSet()

  fun write(
    sourceRoot: SourceRootBuilder.ResolvedSourceRoot,
    parentModuleEntity: ModuleEntity,
    virtualFileUrlManager: VirtualFileUrlManager,
    storage: MutableEntityStorage,
  ) {
    val root = sourceRoot.sourcePath.findVirtualFile()
               ?: sourceRoot.sourcePath.refreshAndFindVirtualFileOrDirectory()
               ?: return
    val newEntities = ArrayList<PackageMarkerEntityBuilder>()
    collect(root, sourceRoot, virtualFileUrlManager, newEntities)
    if (newEntities.isEmpty()) {
      return
    }
    storage.modifyModuleEntity(parentModuleEntity) {
      this.packageMarkerEntities += newEntities
    }
  }

  @Suppress("UnsafeVfsRecursion")
  private fun collect(
    dir: VirtualFile,
    sourceRoot: SourceRootBuilder.ResolvedSourceRoot,
    virtualFileUrlManager: VirtualFileUrlManager,
    into: MutableList<PackageMarkerEntityBuilder>,
  ) {
    if (!dir.isDirectory || dir.`is`(VFileProperty.SYMLINK)) {
      return
    }
    val path = dir.toNioPath()
    if (path in excludedDirectories || !alreadyVisitedDirectories.add(path)) {
      return
    }
    val relativePath = sourceRoot.sourcePath.relativize(path)
    val relativePackagePrefix = relativePath.toString().replace(relativePath.fileSystem.separator, ".")
    into +=
      PackageMarkerEntity(
        root = dir.toVirtualFileUrl(virtualFileUrlManager),
        packagePrefix = concatenatePackages(sourceRoot.packagePrefix, relativePackagePrefix),
        entitySource = BazelDummyEntitySource,
      )
    for (child in dir.children ?: return) {
      collect(child, sourceRoot, virtualFileUrlManager, into)
    }
  }

  companion object {
    // reads excluded directories from the `BazelProjectDirectoriesEntity` already in the storage.
    fun excludedDirectoriesFrom(projectRootDir: Path, dotIdeaPath: Path?, storage: MutableEntityStorage): Set<Path> {
      val excludedDirectoriesFromEntities = storage.entities<BazelProjectDirectoriesEntity>()
        .firstOrNull()?.excludedRoots
        .orEmpty()
        .map { it.url.toPath() }
        .toSet()
      val projectExcludedDirectories = setOf(
        // .idea or alternative project store
        dotIdeaPath ?: projectRootDir.resolve(DIRECTORY_STORE_FOLDER),

        // .bazelbsp
        projectRootDir.resolve(Constants.DOT_BAZELBSP_DIR_NAME),
      )
      return excludedDirectoriesFromEntities + projectExcludedDirectories
    }
  }
}
