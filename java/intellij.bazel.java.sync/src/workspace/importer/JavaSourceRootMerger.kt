package org.jetbrains.bazel.workspace.importer

import com.intellij.openapi.vfs.VFileProperty
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualFileOrDirectory
import com.intellij.openapi.vfs.toNioPathOrNull
import com.intellij.platform.workspace.jps.entities.SourceRootTypeId
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.sync.workspace.languages.jvm.BazelConventionalRoots
import org.jetbrains.bazel.sync.workspace.snapshot.FileToTargetMap
import org.jetbrains.bazel.sync.workspace.snapshot.get
import org.jetbrains.bazel.utils.calculateCommonAncestor
import org.jetbrains.bazel.utils.findVirtualFile
import org.jetbrains.bazel.workspace.importer.SourceRootBuilder.ResolvedSourceRoot
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name

/**
 * Bazel reports every source file of a target individually, and too many single-file source roots are expensive
 * for the workspace model. For one target, this class replaces single-file roots with directory roots where it is
 * non-contradictory, i.e., the directory contains neither JVM sources which are foreign to the target
 * nor resources of the target (they get resource roots of their own).
 *
 * - a real source file is merged into its conventional root, the Java root that [BazelConventionalRoots.javaRootIndex] finds
 *   in its path relative to the target's package root (the directory with the BUILD file).
 *   The root must be non-contradictory, and it must contain no BUILD file and no source file of another target;
 * - the other real source files are grouped by their conventional roots, so a merge never goes above a conventional root.
 *   Each group is merged into the deepest directory which contains all of its files; if that directory is
 *   contradictory, is not strictly below the package root or contains a conventional root of the target,
 *   the files are split by its subdirectories which are merged separately, so files located directly
 *   in the package root or in a contradictory directory are never merged;
 * - a generated source file is merged only into its parent directory;
 * - files shared between several targets are never merged and block merging of their directories;
 * - files which cannot be merged are kept as single-file roots.
 */
@ApiStatus.Internal
class JavaSourceRootMerger(private val fileToTargets: FileToTargetMap) {
  private val virtualFileCache = ConcurrentHashMap<Path, VirtualFile>()

  fun merge(
    baseDirectory: Path,
    sourceRoots: List<ResolvedSourceRoot>,
    resourceFiles: Collection<Path> = emptyList(),
  ): List<ResolvedSourceRoot> {
    if (!BazelFeatureFlags.mergeSourceRoots)
      return sourceRoots

    val (mergeableSourceRoots, keptSourceRoots) = sourceRoots.partition { it.isMergeable() }
    val checker = MergeTargetChecker(mergeableSourceRoots, resourceFiles)
    val (generatedSourceRoots, realSourceRoots) = mergeableSourceRoots.partition { it.generated }
    val decisions = mergeGeneratedSources(generatedSourceRoots, checker) + mergeRealSources(realSourceRoots, baseDirectory, checker)
    return decisions.toSourceRoots() + keptSourceRoots
  }

  private fun ResolvedSourceRoot.isMergeable(): Boolean =
    sourcePath.isRegularFile() &&
    sourcePath.extension in Constants.JVM_LANGUAGES_EXTENSIONS &&
    !sourcePath.isSharedBetweenSeveralTargets()

  /** Merges each generated source into its parent directory, when that directory is non-contradictory. */
  private fun mergeGeneratedSources(sourceRoots: List<ResolvedSourceRoot>, checker: MergeTargetChecker): List<MergeDecision> =
    sourceRoots.map { sourceRoot ->
      MergeDecision(sourceRoot, sourceRoot.sourcePath.parent.takeIf(checker::isNonContradictory))
    }

  /**
   * Merges the real sources into their conventional roots, and the other ones into their deepest directories.
   * A source which is not strictly below [baseDirectory] is kept.
   */
  private fun mergeRealSources(
    sourceRoots: List<ResolvedSourceRoot>,
    baseDirectory: Path,
    checker: MergeTargetChecker,
  ): List<MergeDecision> {
    val (inPackageSourceRoots, outOfPackageSourceRoots) = sourceRoots.partition { it.sourcePath.parent.isStrictlyUnder(baseDirectory) }
    val conventionalRoots = inPackageSourceRoots.associateWith { it.sourcePath.conventionalRoot(baseDirectory) }
    val allRoots = conventionalRoots.values.filterNotNullTo(mutableSetOf())
    val usedRoots = selectConventionalRoots(allRoots, checker)
    val (inUsedRootSourceRoots, otherSourceRoots) = inPackageSourceRoots.partition { it.findUsedRoot(usedRoots) != null }
    return buildList {
      outOfPackageSourceRoots.mapTo(this) { MergeDecision(it, null) }
      inUsedRootSourceRoots.mapTo(this) { MergeDecision(it, it.findUsedRoot(usedRoots)) }
      otherSourceRoots
        .groupBy { conventionalRoots[it] }
        .values
        .flatMapTo(this) { group -> mergeIntoDeepestDirectories(group, baseDirectory, allRoots, checker) }
    }
  }

  /**
   * Returns the Java root of this file, or `null` when the file has no such root.
   * [BazelConventionalRoots.javaRootIndex] takes the segments relative to [baseDirectory], so the root is strictly below it.
   * This file must be strictly below [baseDirectory].
   */
  private fun Path.conventionalRoot(baseDirectory: Path): Path? {
    val segments = baseDirectory.relativize(this).map { it.name }
    val index = BazelConventionalRoots.javaRootIndex(segments) ?: return null
    return segments.take(index + 1).fold(baseDirectory, Path::resolve)
  }

  /**
   * Returns the valid roots of [conventionalRoots] that the merge uses.
   * When one valid root contains another one, only the outer root is used.
   */
  private fun selectConventionalRoots(conventionalRoots: Set<Path>, checker: MergeTargetChecker): List<Path> {
    val usedRoots = mutableListOf<Path>()
    for (root in conventionalRoots.filter(checker::isValidConventionalRoot).sortedBy { it.nameCount }) {
      if (usedRoots.none(root::startsWith)) usedRoots.add(root)
    }
    return usedRoots
  }

  /** A source root under a used root is merged into it, also when its own conventional root differs. */
  private fun ResolvedSourceRoot.findUsedRoot(usedRoots: List<Path>): Path? = usedRoots.firstOrNull(sourcePath::startsWith)

  /**
   * Merges [sourceRoots] into their deepest common directory if it is valid. Otherwise, splits them by the subdirectories
   * of that directory and merges each group separately (each split goes strictly deeper);
   * files located directly in the common directory are kept as is.
   * A directory which contains one of [conventionalRoots] is not valid. Such a directory would go above the conventional root,
   * or it would overlap the source roots of another group.
   */
  private fun mergeIntoDeepestDirectories(
    sourceRoots: List<ResolvedSourceRoot>,
    baseDirectory: Path,
    conventionalRoots: Set<Path>,
    checker: MergeTargetChecker,
  ): List<MergeDecision> {
    if (sourceRoots.isEmpty()) return emptyList()
    // all source roots are strictly under the base directory, so it is their common ancestor at least
    val commonDirectory = sourceRoots.map { it.sourcePath.parent }.reduce { first, second ->
      calculateCommonAncestor(first, second) ?: baseDirectory
    }
    if (commonDirectory.isStrictlyUnder(baseDirectory) &&
        conventionalRoots.none { it.startsWith(commonDirectory) } &&
        checker.isNonContradictory(commonDirectory)) {
      return sourceRoots.map { MergeDecision(it, commonDirectory) }
    }
    return sourceRoots
      .groupBy { sourceRoot ->
        val parent = sourceRoot.sourcePath.parent
        if (parent == commonDirectory) null else commonDirectory.resolve(commonDirectory.relativize(parent).getName(0))
      }
      .flatMap { (subdirectory, group) ->
        if (subdirectory == null) group.map { MergeDecision(it, null) }
        else mergeIntoDeepestDirectories(group, baseDirectory, conventionalRoots, checker)
      }
  }

  /** Replaces the source roots merged into one directory with one root of that directory, and keeps the other ones. */
  private fun List<MergeDecision>.toSourceRoots(): List<ResolvedSourceRoot> {
    val mergedRoots = filter { it.directory != null }
      .groupBy { it.directory to it.sourceRoot.generated }
      .map { (key, decisions) ->
        ResolvedSourceRoot(
          sourcePath = checkNotNull(key.first),
          generated = key.second,
          rootType = decisions.map { it.sourceRoot.rootType }.majorityRootType(),
        )
      }
    val keptRoots = filter { it.directory == null }.map { it.sourceRoot }
    return mergedRoots + keptRoots
  }

  private fun Path.isStrictlyUnder(directory: Path): Boolean = this != directory && startsWith(directory)

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

  /**
   * Checks the merge targets of one [merge] call.
   * The source files of [mergeableSourceRoots] are known to the target, and [resourceFiles] are the resources of the target.
   */
  private inner class MergeTargetChecker(
    mergeableSourceRoots: List<ResolvedSourceRoot>,
    resourceFiles: Collection<Path>,
  ) {
    private val knownFiles = mergeableSourceRoots.mapNotNullTo(mutableSetOf()) { it.sourcePath.findOrRefreshVirtualFile() }
    private val resourceFileSet = resourceFiles.toSet()
    private val contradictoryFileFinder = ConflictingFileFinder { file ->
      file.extension in Constants.JVM_LANGUAGES_EXTENSIONS && file !in knownFiles ||
      resourceFileSet.isNotEmpty() && file.toNioPath() in resourceFileSet
    }
    private val foreignFileFinder = ConflictingFileFinder { file ->
      file.name in Constants.BUILD_FILE_NAMES || file !in knownFiles && fileToTargets[file.toNioPath()].isNotEmpty()
    }

    /** The directory contains no foreign JVM file and no resource of the target. */
    fun isNonContradictory(directory: Path): Boolean = directory.containsNo(contradictoryFileFinder)

    /** The directory is non-contradictory, and it contains no BUILD file and no source of another target. */
    fun isValidConventionalRoot(directory: Path): Boolean = isNonContradictory(directory) && directory.containsNo(foreignFileFinder)

    private fun Path.containsNo(finder: ConflictingFileFinder): Boolean {
      val directory = findOrRefreshVirtualFile() ?: return false
      return !finder.containsConflictingFile(directory)
    }
  }
}

/** The directory that [sourceRoot] is merged into, or `null` when [sourceRoot] is kept as is. */
private data class MergeDecision(val sourceRoot: ResolvedSourceRoot, val directory: Path?)

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
 *
 * A stale VFS can still list a deleted file. So a file that [isConflicting] accepts is a conflict only when it exists on disk.
 * The disk check runs only for such a file, so it does not touch the disk for the regular files of a tree.
 */
private class ConflictingFileFinder(private val isConflicting: (VirtualFile) -> Boolean) {
  private val cache = HashMap<VirtualFile, Boolean>()

  fun containsConflictingFile(file: VirtualFile): Boolean = cache.getOrPut(file) {
    @Suppress("UnsafeVfsRecursion") // directory symlinks are ignored, so it's safe
    when {
      file.isDirectory -> file.children.orEmpty().any { child ->
        if (child.isDirectory) !child.`is`(VFileProperty.SYMLINK) && containsConflictingFile(child) else child.isConflictingOnDisk()
      }

      else -> file.isConflictingOnDisk()
    }
  }

  /** A file of another file system than the local one has no disk path, so it is a conflict when [isConflicting] accepts it. */
  private fun VirtualFile.isConflictingOnDisk(): Boolean = isConflicting(this) && toNioPathOrNull()?.exists() != false
}
