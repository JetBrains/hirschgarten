package org.jetbrains.bazel.workspace

import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndexContributor
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileKind
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetRegistrar
import com.intellij.workspaceModel.ide.toPath
import org.jetbrains.bazel.sdkcompat.CONTENT_NON_INDEXABLE_SUPPORTED
import org.jetbrains.bazel.sdkcompat.registerOtherRootsCompat
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.BazelProjectDirectoriesEntity

class BazelProjectDirectoriesWorkspaceFileIndexContributor : WorkspaceFileIndexContributor<BazelProjectDirectoriesEntity> {
  override val entityClass: Class<BazelProjectDirectoriesEntity> = BazelProjectDirectoriesEntity::class.java

  override fun registerFileSets(
    entity: BazelProjectDirectoriesEntity,
    registrar: WorkspaceFileSetRegistrar,
    storage: EntityStorage,
  ) {
    /**
     * In IDEA 2025.2, [registerOtherRootsCompat] registers a [WorkspaceFileKind.CONTENT_NON_INDEXABLE] file set at the project root.
     * This makes files under the project root part of the project without actually having to index them
     * (which can be very slow if there files other than source files, see https://youtrack.jetbrains.com/issue/BAZEL-2088).
     * If for some reason we do want to index files that aren't part of any target, then we can call [registerIncludedDirectories].
     * Conversely, in IDEA 2025.1, where [WorkspaceFileKind.CONTENT_NON_INDEXABLE] isn't available, we have to do it regardless.
     */
    @Suppress("SimplifyBooleanWithConstants")
    if (entity.indexAllFilesInIncludedRoots || !CONTENT_NON_INDEXABLE_SUPPORTED) {
      registrar.registerIncludedDirectories(entity)
    } else {
      registrar.registerBuildFiles(entity)
    }
    registrar.registerExcludedDirectories(entity)
    registrar.registerOtherRootsCompat(entity.projectRoot, entity.includedRoots, entity)
  }

  private fun WorkspaceFileSetRegistrar.registerIncludedDirectories(entity: BazelProjectDirectoriesEntity) =
    entity.includedRoots.forEach {
      registerFileSet(
        root = it,
        kind = WorkspaceFileKind.CONTENT,
        entity = entity,
        customData = null,
      )
    }

  private fun WorkspaceFileSetRegistrar.registerExcludedDirectories(entity: BazelProjectDirectoriesEntity) {
    excludeSymlinksFromFileWatcher(entity.excludedRoots.map { it.toPath() })
    entity.excludedRoots.forEach {
      registerExcludedRoot(
        excludedRoot = it,
        entity = entity,
      )
    }
  }

  private fun WorkspaceFileSetRegistrar.registerBuildFiles(entity: BazelProjectDirectoriesEntity) =
    entity.buildFiles.forEach {
      registerNonRecursiveFileSet(
        file = it,
        kind = WorkspaceFileKind.CONTENT,
        entity = entity,
        customData = null,
      )
    }
}
