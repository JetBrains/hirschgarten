package org.jetbrains.bazel.workspace.importer

import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.platform.workspace.jps.entities.LibraryEntity
import com.intellij.platform.workspace.storage.MutableEntityStorage
import com.intellij.platform.workspace.storage.entities
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.sync.workspace.languages.java.sourceRoot.JvmPackagePrefixCalculator
import org.jetbrains.bazel.workspacemodel.entities.BazelProjectEntitySource
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeEntity
import org.jetbrains.bazel.workspacemodel.entities.CompiledSourceCodeInsideJarExcludeId
import org.jetbrains.bazel.workspacemodel.entities.LibraryCompiledSourceCodeInsideJarExcludeEntity
import org.jetbrains.bsp.protocol.LibraryItem
import org.jetbrains.bsp.protocol.RawBuildTarget
import java.util.Locale
import kotlin.io.path.invariantSeparatorsPathString

// https://youtrack.jetbrains.com/issue/BAZEL-1672
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
   * its ID is reused; otherwise the ID is bumped so referring entities invalidate and
   * `CompiledSourceCodeInsideJarExcludeWorkspaceFileIndexContributor` re-runs.
   */
  fun write(
    targets: Collection<RawBuildTarget>,
    libraries: List<LibraryItem>,
    packagePrefixes: JvmPackagePrefixCalculator,
    storage: MutableEntityStorage,
    currentExcludeEntity: CompiledSourceCodeInsideJarExcludeEntity? = null,
  ) {
    val librariesFromInternalTargetsUrls = calculateLibrariesFromInternalTargetsUrls(libraries)
    if (librariesFromInternalTargetsUrls.isEmpty()) {
      return
    }

    val relativePathsInsideJarToExclude = calculateRelativePathsInsideJarToExclude(targets, packagePrefixes)

    val excludeEntityId =
      if (currentExcludeEntity == null) {
        0
      } else if (currentExcludeEntity.relativePathsInsideJarToExclude == relativePathsInsideJarToExclude &&
        currentExcludeEntity.librariesFromInternalTargetsUrls == librariesFromInternalTargetsUrls
      ) {
        currentExcludeEntity.excludeId.id
      } else {
        // Change the ID so that all referring entities (LibraryCompiledSourceCodeInsideJarExcludeEntity)
        // are invalidated and CompiledSourceCodeInsideJarExcludeWorkspaceFileIndexContributor re-runs on them.
        currentExcludeEntity.excludeId.id + 1
      }

    val excludeEntity = storage.addEntity(
      CompiledSourceCodeInsideJarExcludeEntity(
        relativePathsInsideJarToExclude = relativePathsInsideJarToExclude,
        librariesFromInternalTargetsUrls = librariesFromInternalTargetsUrls,
        excludeId = CompiledSourceCodeInsideJarExcludeId(excludeEntityId),
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

  private fun calculateRelativePathsInsideJarToExclude(
    targets: Collection<RawBuildTarget>,
    packagePrefixes: JvmPackagePrefixCalculator,
  ): Set<String> {
    val result = HashSet<String>()
    for (target in targets) {
      val jvmPackagePrefixes = packagePrefixes.get(target)
      for (sourceRoot in target.sources) {
        if (sourceRoot.generated) continue

        val sourceName = sourceRoot.path.fileName.toString()
        val classNames =
          when {
            sourceName.endsWith(".java") -> listOf(sourceName, sourceName.removeSuffix(".java") + ".class")
            sourceName.endsWith(".kt") -> {
              val withoutExtension = sourceName.removeSuffix(".kt")
              // E.g., main.kt -> MainKt.class
              val kotlinFileClassName = "${withoutExtension.capitalizeAscii()}Kt.class"
              listOf(sourceName, "$withoutExtension.class", kotlinFileClassName)
            }
            else -> continue
          }

        val packagePrefix = jvmPackagePrefixes[sourceRoot.path]?.replace(".", "/") ?: ""
        classNames.forEach { className ->
          result.add(if (packagePrefix.isNotEmpty()) "$packagePrefix/$className" else className)
        }
      }
    }
    return result
  }

  private fun calculateLibrariesFromInternalTargetsUrls(libraryItems: List<LibraryItem>): Set<String> =
    libraryItems
      .asSequence()
      .filter { it.containsInternalJars }
      .flatMap { it.jars.asSequence() + it.ijars.asSequence() + it.sourceJars.asSequence() }
      .map { jarPath -> JarFileSystem.PROTOCOL_PREFIX + jarPath.invariantSeparatorsPathString + JarFileSystem.JAR_SEPARATOR }
      .toSet()

  private fun String.capitalizeAscii(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }
}
