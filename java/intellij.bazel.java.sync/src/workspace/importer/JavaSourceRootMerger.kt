package org.jetbrains.bazel.workspace.importer

import com.intellij.openapi.vfs.VFileProperty
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualFileOrDirectory
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.sync.workspace.snapshot.FileToTargetMap
import org.jetbrains.bazel.sync.workspace.snapshot.get
import org.jetbrains.bazel.utils.calculateCommonAncestor
import org.jetbrains.bazel.utils.findVirtualFile
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

/**
 * Bazel reports every source file of a target individually, and too many single-file source roots are expensive
 * for the workspace model. For one target, this class replaces single-file roots with directory roots where it is
 * non-contradictory, i.e., the directory contains neither JVM sources which are foreign to the target
 * nor resources of the target (they get resource roots of their own).
 *
 * - real source files are merged into the deepest directory which contains all of them; if that directory is
 *   contradictory or is not strictly below the target's package root (the directory with the BUILD file),
 *   the files are split by its subdirectories which are merged separately, so files located directly
 *   in the package root or in a contradictory directory are never merged;
 * - a generated source file is merged only into its parent directory;
 * - files shared between several targets are never merged and block merging of their directories;
 * - files which cannot be merged are kept as single-file roots.
 */
@ApiStatus.Internal
class JavaSourceRootMerger(
  private val fileToTargets: FileToTargetMap,
) {
  private val virtualFileCache = ConcurrentHashMap<Path, VirtualFile>()

  fun merge(
    baseDirectory: Path,
    sourceRoots: List<SourceRootBuilder.ResolvedSourceRoot>,
    resourceFiles: Collection<Path> = emptyList(),
  ): List<SourceRootBuilder.ResolvedSourceRoot> {
    if (!BazelFeatureFlags.mergeSourceRoots)
      return sourceRoots

    val (mergeableSourceRoots, keptSourceRoots) = sourceRoots.partition { it.isMergeable() }
    val finder = UnknownFileFinder(
      knownFiles = mergeableSourceRoots.mapNotNullTo(mutableSetOf()) { it.sourcePath.findOrRefreshVirtualFile() },
      conflictingFiles = resourceFiles.toSet(),
      relevantExtensions = Constants.JVM_LANGUAGES_EXTENSIONS,
    )
    val result = MergeResult()

    val (generatedSourceRoots, realSourceRoots) = mergeableSourceRoots.partition { it.generated }
    for (sourceRoot in generatedSourceRoots) {
      val directory = sourceRoot.sourcePath.parent
      if (directory.isValidMergeTarget(finder)) result.merge(directory, listOf(sourceRoot)) else result.keep(listOf(sourceRoot))
    }

    val (inPackageSourceRoots, outOfPackageSourceRoots) = realSourceRoots.partition { it.sourcePath.parent.isStrictlyUnder(baseDirectory) }
    result.keep(outOfPackageSourceRoots)
    mergeIntoDeepestDirectories(inPackageSourceRoots, baseDirectory, finder, result)

    return buildList {
      addAll(result.mergedSourceRoots())
      addAll(result.notMerged)
      addAll(keptSourceRoots)
    }
  }

  private fun SourceRootBuilder.ResolvedSourceRoot.isMergeable(): Boolean =
    sourcePath.isRegularFile() &&
    sourcePath.extension in Constants.JVM_LANGUAGES_EXTENSIONS &&
    !sourcePath.isSharedBetweenSeveralTargets()

  /**
   * Merges [sourceRoots] into their deepest common directory if it is valid. Otherwise, splits them by the subdirectories
   * of that directory and merges each group separately (each split goes strictly deeper);
   * files located directly in the common directory are kept as is.
   */
  private fun mergeIntoDeepestDirectories(
    sourceRoots: List<SourceRootBuilder.ResolvedSourceRoot>,
    baseDirectory: Path,
    finder: UnknownFileFinder,
    result: MergeResult,
  ) {
    if (sourceRoots.isEmpty()) return
    // all source roots are strictly under the base directory, so it is their common ancestor at least
    val commonDirectory = sourceRoots.map { it.sourcePath.parent }.reduce { first, second ->
      calculateCommonAncestor(first, second) ?: baseDirectory
    }
    if (commonDirectory.isStrictlyUnder(baseDirectory) && commonDirectory.isValidMergeTarget(finder)) {
      result.merge(commonDirectory, sourceRoots)
      return
    }
    val bySubdirectory = sourceRoots.groupBy { sourceRoot ->
      val parent = sourceRoot.sourcePath.parent
      if (parent == commonDirectory) null else commonDirectory.resolve(commonDirectory.relativize(parent).getName(0))
    }
    for ((subdirectory, group) in bySubdirectory) {
      if (subdirectory == null) {
        result.keep(group)
      }
      else {
        mergeIntoDeepestDirectories(group, baseDirectory, finder, result)
      }
    }
  }

  private fun Path.isStrictlyUnder(directory: Path): Boolean = this != directory && startsWith(directory)

  private fun Path.isValidMergeTarget(finder: UnknownFileFinder): Boolean {
    val directory = findOrRefreshVirtualFile() ?: return false
    return !finder.containsUnknownMemo(directory)
  }

  /**
   * A shared file can't be merged: other targets would create overlapping source roots for it, and IDEA only considers
   * the innermost source root, which causes red code, e.g., on https://github.com/bazelbuild/bazel.
   */
  private fun Path.isSharedBetweenSeveralTargets(): Boolean = fileToTargets[this]
                                                                .distinctBy { it.stripAspects() }
                                                                .count() > 1

    private val rootCardinalityComparator = compareBy<Map.Entry<SourceRootTypeId, Int>> { it.value }
    .thenBy { it.key == JAVA_TEST_SOURCE_ROOT_TYPE }

  /** Prefer test roots on ties (e.g., if some utility target is in the same package as a java_test). */
  private fun List<SourceRootTypeId>.majorityRootType(): SourceRootTypeId =
    groupingBy { it }
      .eachCount()
      .entries
      .maxWith(rootCardinalityComparator)
      .key

  private fun Path.findOrRefreshVirtualFile(): VirtualFile? = virtualFileCache.getOrPut(this) {
    this.findVirtualFile() ?: this.refreshAndFindVirtualFileOrDirectory() ?: return null
  }

  private inner class MergeResult {
    // (directory, generated) -> source roots merged into it
    private val merged = LinkedHashMap<Pair<Path, Boolean>, MutableList<SourceRootBuilder.ResolvedSourceRoot>>()
    val notMerged = mutableListOf<SourceRootBuilder.ResolvedSourceRoot>()

    fun merge(directory: Path, sourceRoots: List<SourceRootBuilder.ResolvedSourceRoot>) {
      merged.getOrPut(directory to sourceRoots.first().generated) { mutableListOf() }.addAll(sourceRoots)
    }

    fun keep(sourceRoots: List<SourceRootBuilder.ResolvedSourceRoot>) {
      notMerged.addAll(sourceRoots)
    }

    fun mergedSourceRoots(): List<SourceRootBuilder.ResolvedSourceRoot> = merged.map { (key, sourceRoots) ->
      SourceRootBuilder.ResolvedSourceRoot(
        sourcePath = key.first,
        generated = key.second,
        rootType = sourceRoots.map { it.rootType }.majorityRootType(),
      )
    }
  }
}

/**
 * Memoize file tree traversal, avoids unnecessary recursively traversing file tree,
 * before time complexity was O(N * M * K), where N is amount of source roots, M
 * is average prefix length and K is amount of files in subtree.
 *
 * This simple memoization reduce that to O(N) where N is size
 * of some file subtree (hard to actually define what size subtree)
 *
 * Before optimization issue was especially apparent for LSP where VFS for
 * workspace importers is NOT cached and each VFS IO operation touches real file system.
 */
private class UnknownFileFinder(
  private val knownFiles: Set<VirtualFile>,
  private val conflictingFiles: Set<Path>,
  private val relevantExtensions: List<String>,
) {
  private val cache = HashMap<VirtualFile, Boolean>()

  fun containsUnknownMemo(file: VirtualFile): Boolean = cache.getOrPut(file) {
    @Suppress("UnsafeVfsRecursion") // symlinks are ignored, so it's safe
    when {
      file.isDirectory -> file.children.orEmpty().any { child ->
        !child.`is`(VFileProperty.SYMLINK) && if (child.isDirectory) containsUnknownMemo(child) else child.isUnknown()
      }

      else -> file.isUnknown()
    }
  }

  private fun VirtualFile.isUnknown(): Boolean =
    extension in relevantExtensions && this !in knownFiles ||
    conflictingFiles.isNotEmpty() && toNioPath() in conflictingFiles
}
