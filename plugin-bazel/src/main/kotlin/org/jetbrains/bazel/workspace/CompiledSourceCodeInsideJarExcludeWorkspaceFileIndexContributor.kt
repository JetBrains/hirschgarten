package org.jetbrains.bazel.workspace

import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.platform.workspace.jps.entities.LibraryRootTypeId
import com.intellij.platform.workspace.storage.EntityStorage
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileIndexContributor
import com.intellij.workspaceModel.core.fileIndex.WorkspaceFileSetRegistrar
import org.jetbrains.bazel.sdkcompat.workspacemodel.entities.LibraryCompiledSourceCodeInsideJarExcludeEntity
import kotlin.collections.contains

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

/**
 * There's some cases where we don't want to index certain files inside a jar, either because of performance considerations or to
 * prevent incorrect resolve. CompiledSourceCodeInsideJarExcludeWorkspaceFileIndexContributor adds conditional excludes for such files.
 *
 * 1. Oftentimes, jars can contain resources, e.g., XML/HTML/Javascript/etc.
 *    We don't index resources inside jars in the following cases (by using [JVM_EXTENSIONS] as a filter):
 *    1. It is an "internal jar", i.e., an output jar of an internal target. Here "internal" usually refers to
 *       the "main workspace" (i.e., a label that starts with "//"), but can also be something like "@community//" in the case of Ultimate.
 *       Because internal jars are built from source files that we already have added to the project,
 *       resolve should use those local files (be it XML or HTML or whatever) anyway. By skipping those resources during indexing we index less and save time.
 *
 *       *Note:* for external libraries there exist valid cases where resources DO need to be indexed inside jars. E.g., pre-built IntelliJ plugins
 *       contain plugin.xml files that need to be indexed in order for the XML references to resolve properly.
 *    2. In source jars. Source jars are supposed to contain, well, source code, e.g., `.java`/`.kt` files.
 *       Still, source jars can contain HTML/JS/other junk that slows down indexing for no good reason.
 * 2. We don't index `.class` and `.java`/`.kt`/`.scala` files inside jars if there's already a source file in the project with the same
 *    fully-qualified class name. [CompiledSourceCodeInsideJarExcludeEntityUpdater] takes in the source files and computes the
 *    relative paths that we don't have to index. E.g., if there's a Java source file that defines a class with the following FQN:
 *    `com.example.Example`, then we will skip `*.jar!/com/example/Example.class` during indexing.
 *    This is done mainly to prevent resolve from navigating into jars instead of source code (see https://youtrack.jetbrains.com/issue/BAZEL-1672),
 *    but this also helps with indexing performance.
 */
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
            return@registerExclusionCondition virtualFile.hasNonJvmExtension()
          }
          false
        },
        entity = entity,
      )

      if (libraryRoot.type == LibraryRootTypeId.SOURCES) {
        registrar.registerExclusionCondition(
          root = contentRootUrl,
          condition = { virtualFile ->
            virtualFile.hasNonJvmExtension()
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

private fun VirtualFile.hasNonJvmExtension() = isFile && extension !in JVM_EXTENSIONS
