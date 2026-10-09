package org.jetbrains.bazel.workspace

import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndexContributor
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileKind
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetRegistrar
import com.intellij.workspaceModel.ide.toPath
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectDirectoriesEntity

/**
 * Registers the file sets of [BazelProjectDirectoriesEntity].
 *
 * The project root is non-indexable content.
 * Thus, all the files under it are part of the project, but the IDE does not index them.
 * Indexing of the files other than the sources can be very slow, see https://youtrack.jetbrains.com/issue/BAZEL-2088.
 *
 * [org.jetbrains.bazel.workspace.indexing.IndexableContentCollector] selects the files to index in addition to the target sources.
 * These are the files that match the `index` patterns, the Bazel files, the root workspace files and the project view file.
 * They also include the files from [org.jetbrains.bazel.workspace.indexing.IndexableContentContributor].
 * [registerIndexableRecursiveRoots] indexes the directories that the patterns match recursively.
 * [registerIndexableNonRecursiveRoots] indexes each of the other selected files.
 */
internal class BazelProjectDirectoriesWorkspaceFileIndexContributor : WorkspaceFileIndexContributor<BazelProjectDirectoriesEntity> {
  override val entityClass: Class<BazelProjectDirectoriesEntity> = BazelProjectDirectoriesEntity::class.java

  override fun registerFileSets(
    entity: BazelProjectDirectoriesEntity,
    registrar: WorkspaceFileSetRegistrar,
    storage: EntityStorage,
  ) {
    registrar.registerIndexableRecursiveRoots(entity)
    registrar.registerIndexableNonRecursiveRoots(entity)
    registrar.registerExcludedDirectories(entity)

    registrar.registerFileSet(
      root = entity.projectRoot,
      kind = WorkspaceFileKind.CONTENT_NON_INDEXABLE,
      entity = entity,
      customData = null,
    )
  }

  private fun WorkspaceFileSetRegistrar.registerIndexableRecursiveRoots(entity: BazelProjectDirectoriesEntity) =
    entity.indexableRecursiveRoots.forEach {
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

  private fun WorkspaceFileSetRegistrar.registerIndexableNonRecursiveRoots(entity: BazelProjectDirectoriesEntity) {
    entity.indexableNonRecursiveRoots.forEach {
      registerNonRecursiveFileSet(
        file = it,
        kind = WorkspaceFileKind.CONTENT,
        entity = entity,
        customData = null,
      )
    }
  }
}
