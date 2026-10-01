package org.jetbrains.bazel.workspace.importer

import com.google.common.hash.Hashing
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.entities
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeId
import org.jetbrains.bazel.workspacemodel.entities.LibraryCompiledSourceCodeInsideJarExcludeEntity
import org.jetbrains.bsp.protocol.BuildTarget
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.OutputLocation
import org.jetbrains.bsp.protocol.nonGeneratedSources
import java.nio.file.Path
import java.util.Locale
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString

// https://youtrack.jetbrains.com/issue/BAZEL-1672
// RC: replaces `CompiledSourceCodeInsideJarExcludeTransformer` + `CompiledSourceCodeInsideJarExcludeEntityUpdater`
@ApiStatus.Internal
object CompiledSourceCodeInsideJarExcludeBuilder {
  /**
   * writes a [CompiledSourceCodeInsideJarExcludeEntity] plus a
   * [LibraryCompiledSourceCodeInsideJarExcludeEntity] per existing [LibraryEntity] in [storage].
   *
   * returns without writing anything when there are no libraries from internal targets that need
   * source-code exclusion.
   *
   * must run after [LibraryBuilder] so the per-library link entities have something to reference.
   *
   * [currentExcludeEntity] is the previous exclude entity (if any). when the content is unchanged,
   * its ID is reused; otherwise the ID is a hash of the content, so referring entities invalidate and
   * `CompiledSourceCodeInsideJarExcludeWorkspaceFileIndexContributor` re-runs.
   * the contributor caches data by the ID, so the ID must be different for different content
   * also among the open projects and the IDs loaded from the workspace model cache.
   */
  fun write(
    targets: Collection<BuildTarget>,
    libraries: List<LibraryItem>,
    resolveLocation: (OutputLocation) -> Path?,
    storage: MutableEntityStorage,
    currentExcludeEntity: CompiledSourceCodeInsideJarExcludeEntity? = null,
  ) {
    val librariesFromInternalTargetsUrls = calculateLibrariesFromInternalTargetsUrls(libraries)
    if (librariesFromInternalTargetsUrls.isEmpty()) {
      return
    }

    val relativePathsInsideJarToExclude = calculateSourceFilePaths(targets, resolveLocation)

    val nextExcludeEntityId: CompiledSourceCodeInsideJarExcludeId =
      if (currentExcludeEntity != null &&
          currentExcludeEntity.relativePathsInsideJarToExclude == relativePathsInsideJarToExclude &&
          currentExcludeEntity.librariesFromInternalTargetsUrls == librariesFromInternalTargetsUrls
      ) {
        currentExcludeEntity.excludeId
      } else {
        // Change the ID so that all referring entities (LibraryCompiledSourceCodeInsideJarExcludeEntity)
        // are invalidated and CompiledSourceCodeInsideJarExcludeWorkspaceFileIndexContributor re-runs on them.
        contentExcludeId(relativePathsInsideJarToExclude, librariesFromInternalTargetsUrls)
      }

    val excludeEntity = storage.addEntity(
      CompiledSourceCodeInsideJarExcludeEntity(
        relativePathsInsideJarToExclude = relativePathsInsideJarToExclude,
        librariesFromInternalTargetsUrls = librariesFromInternalTargetsUrls,
        excludeId = nextExcludeEntityId,
        entitySource = BazelProjectEntitySource,
      ),
    )

    storage.entities<LibraryEntity>().forEach { library ->
      storage.addEntity(
        LibraryCompiledSourceCodeInsideJarExcludeEntity(
          libraryId = library.symbolicId,
          compiledSourceCodeInsideJarExcludeId = excludeEntity.symbolicId,
          entitySource = BazelProjectEntitySource,
        ),
      )
    }
  }

  /** The same content gives the same ID, so the ID does not depend on the order of the syncs or on the open projects. */
  private fun contentExcludeId(sourceFilePaths: Set<String>, libraryUrls: Set<String>): CompiledSourceCodeInsideJarExcludeId {
    val hasher = Hashing.murmur3_32_fixed().newHasher()
    for (values in listOf(sourceFilePaths, libraryUrls)) {
      for (value in values.sorted()) {
        hasher.putString(value, Charsets.UTF_8)
        hasher.putByte(0)
      }
      hasher.putByte(1)
    }
    return CompiledSourceCodeInsideJarExcludeId(hasher.hash().asInt())
  }

  /**
   * The package of a source file is unknown, so the paths of the source files are stored instead of the paths inside jars.
   * [CompiledSourceFileIndex] approximates the package by the relative path inside the jar and matches it against them.
   */
  @VisibleForTesting
  fun calculateSourceFilePaths(
    targets: Collection<BuildTarget>,
    resolveLocation: (OutputLocation) -> Path?,
  ): Set<String> {
    val result = HashSet<String>()
    for (target in targets) {
      for (source in target.nonGeneratedSources().mapNotNull(resolveLocation)) {
        if (source.extension == "java" || source.extension == "kt") {
          result.add(source.invariantSeparatorsPathString)
        }
      }
    }
    return result
  }

  @VisibleForTesting
  fun calculateLibrariesFromInternalTargetsUrls(libraryItems: List<LibraryItem>): Set<String> =
    libraryItems
      .asSequence()
      .filter { it.containsInternalJars }
      .flatMap { it.jars.asSequence() + it.ijars.asSequence() + it.sourceJars.asSequence() }
      .map { jarPath -> JarFileSystem.PROTOCOL_PREFIX + jarPath.invariantSeparatorsPathString + JarFileSystem.JAR_SEPARATOR }
      .toSet()
}

/**
 * Tells whether a file inside a jar is compiled from (or is a copy of) a source file of the project.
 *
 * The package of the file inside the jar is approximated by its relative path inside the jar:
 * `com/example/Foo.class` corresponds to any source file `.../com/example/Foo.java`.
 */
@ApiStatus.Internal
class CompiledSourceFileIndex(sourceFilePaths: Collection<String>) {
  // source file name -> directories which contain a source file with this name
  private val directoriesByFileName: Map<String, List<String>> =
    sourceFilePaths.groupBy({ it.substringAfterLast('/') }, { it.substringBeforeLast('/', "") })

  /**
   * [relativePathInsideJar] must not point at a nested class, e.g., `com/example/Foo$Bar.class` should be passed as
   * `com/example/Foo.class`.
   */
  fun hasSourceFor(relativePathInsideJar: String): Boolean {
    val packageDirectory = relativePathInsideJar.substringBeforeLast('/', "")
    val fileName = relativePathInsideJar.substringAfterLast('/')
    return sourceFileNameCandidates(fileName).any { sourceFileName ->
      directoriesByFileName[sourceFileName]?.any { it.endsWithPackageDirectory(packageDirectory) } == true
    }
  }

  private fun sourceFileNameCandidates(fileName: String): List<String> {
    val nameWithoutExtension = fileName.substringBeforeLast('.')
    return when (fileName.substringAfterLast('.', "")) {
      "java", "kt" -> listOf(fileName)
      "class" -> buildList {
        add("$nameWithoutExtension.java")
        add("$nameWithoutExtension.kt")
        // E.g., MainKt.class -> main.kt or Main.kt
        val kotlinFileName = nameWithoutExtension.removeSuffix("Kt")
        if (kotlinFileName != nameWithoutExtension && kotlinFileName.isNotEmpty()) {
          add("$kotlinFileName.kt")
          add("${kotlinFileName.replaceFirstChar { it.lowercase(Locale.US) }}.kt")
        }
      }
      else -> emptyList()
    }
  }

  private fun String.endsWithPackageDirectory(packageDirectory: String): Boolean =
    packageDirectory.isEmpty() || this == packageDirectory || endsWith("/$packageDirectory")
}
