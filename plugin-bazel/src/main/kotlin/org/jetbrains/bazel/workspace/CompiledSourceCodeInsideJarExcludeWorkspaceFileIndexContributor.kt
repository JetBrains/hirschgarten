package org.jetbrains.bazel.workspace

import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndexContributor
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetRegistrar
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.LibraryCompiledSourceCodeInsideJarExcludeEntity

/**
 * Don't index irrelevant files inside jars that are built from internal targets.
 * We care about generated classes (and respective generated sources), but not about generated resources.
 */
private val ALLOWED_FILE_EXTENSIONS_IN_LIBRARIES_FROM_INTERNAL_TARGETS =
  listOf(
    "class",
    "java",
    "kt",
    "scala",
  )

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
    val librariesFromInternalTargetsUrls: Set<String> = compiledSourceCodeInsideJarExcludeEntity.librariesFromInternalTargetsUrls

    library.roots.map { libraryRoot ->
      val contentRootUrl = libraryRoot.url
      registrar.registerExclusionCondition(
        root = contentRootUrl,
        condition = { virtualFile ->
          if (virtualFile.isDirectory) return@registerExclusionCondition false
          val rootFile = VfsUtilCore.getRootFile(virtualFile)
          if (virtualFile.getRelativePathInsideJar(rootFile) in relativePathsToExclude) return@registerExclusionCondition true
          if (rootFile.url in librariesFromInternalTargetsUrls) {
            return@registerExclusionCondition virtualFile.extension !in ALLOWED_FILE_EXTENSIONS_IN_LIBRARIES_FROM_INTERNAL_TARGETS
          }
          false
        },
        entity = entity,
      )
    }
  }
}

/**
 * Based on `ArchiveFileSystem#getRelativePath`
 */
private fun VirtualFile.getRelativePathInsideJar(rootFile: VirtualFile): String {
  val relativePath: String = this.path.substring(rootFile.path.length)
  return StringUtil.trimLeading(relativePath, '/')
}
