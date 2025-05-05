package org.jetbrains.bazel.workspace

import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndexContributor
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetRegistrar
import org.jetbrains.bazel.workspacemodel.entities.LibraryCompiledSourceCodeInsideJarExcludeEntity

class CompiledSourceCodeInsideJarExcludeWorkspaceFileIndexContributor :
  WorkspaceFileIndexContributor<LibraryCompiledSourceCodeInsideJarExcludeEntity> {
  override val entityClass: Class<LibraryCompiledSourceCodeInsideJarExcludeEntity>
    get() = LibraryCompiledSourceCodeInsideJarExcludeEntity::class.java

  override fun registerFileSets(
    entity: LibraryCompiledSourceCodeInsideJarExcludeEntity,
    registrar: WorkspaceFileSetRegistrar,
    storage: EntityStorage,
  ) {
    val library = storage.resolve(entity.libraryId) ?: return
    val compiledSourceCodeInsideJarExcludeEntity = storage.resolve(entity.compiledSourceCodeInsideJarExcludeId) ?: return

    val relativePathsToExclude: Set<String> = compiledSourceCodeInsideJarExcludeEntity.relativePathsInsideJarToExclude

    library.roots.map { libraryRoot ->
      val contentRootUrl = libraryRoot.url
      registrar.registerExclusionCondition(
        root = contentRootUrl,
        condition = {
          it.getRelativePathInsideJar() in relativePathsToExclude
        },
        entity = entity,
      )
    }
  }
}

/**
 * Copied from `ArchiveFileSystem#getRelativePath` which is not `public` for some reason
 */
private fun VirtualFile.getRelativePathInsideJar(): String {
  val relativePath: String = this.path.substring(VfsUtilCore.getRootFile(this).path.length)
  return StringUtil.trimLeading(relativePath, '/')
}
