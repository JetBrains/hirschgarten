package org.jetbrains.bazel.workspace

import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndexContributor
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetRegistrar
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.LibraryCompiledSourceCodeInsideJarExcludeEntity

/**
 * Don't index irrelevant files inside jars that are built from internal targets.
 * We care about generated classes (and respective generated sources), but not about generated resources.
 */
private val JVM_EXTENSIONS =
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
          val relativePath = virtualFile.getRelativePathInsideJar(rootFile)
          val relativePathWithoutNestedClass = removeNestedClass(relativePath)
          if (relativePathWithoutNestedClass in relativePathsToExclude) return@registerExclusionCondition true
          if (rootFile.url in librariesFromInternalTargetsUrls) {
            return@registerExclusionCondition virtualFile.extension !in JVM_EXTENSIONS
          }
          false
        },
        entity = entity,
      )

      if (libraryRoot.type == LibraryRootTypeId.SOURCES) {
        registrar.registerExclusionCondition(
          root = contentRootUrl,
          condition = { virtualFile ->
            virtualFile.extension !in JVM_EXTENSIONS
          },
          entity = entity,
        )
      }
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

/**
 * example/lib.jar!/Example$1.class -> example/lib.jar!/Example.class
 */
private fun removeNestedClass(relativePath: String): String {
  val dollarIndex = relativePath.indexOf('$')
  if (dollarIndex == -1) return relativePath
  val dotIndex = relativePath.indexOf('.', dollarIndex + 1)
  if (dotIndex == -1) return relativePath
  return relativePath.substring(0, dollarIndex) + relativePath.substring(dotIndex)
}
