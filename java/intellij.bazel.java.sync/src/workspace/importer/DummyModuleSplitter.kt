package org.jetbrains.bazel.workspace.importer

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.VFileProperty
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.refreshAndFindVirtualFileOrDirectory
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.magicmetamodel.sanitizeName
import org.jetbrains.bazel.magicmetamodel.shortenTargetPath
import org.jetbrains.bazel.sync.workspace.snapshot.File2TargetMap
import org.jetbrains.bazel.sync.workspace.snapshot.get
import org.jetbrains.bazel.utils.findVirtualFile
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.pathString

private val log = logger<DummyModuleSplitter>()

/**
 * This is a HACK for letting single source Java files to be resolved normally.
 * should be removed soon and replaced with a more robust solution.
 *
 * Decides, for one target, whether its source roots should be:
 *   - merged into a smaller set of broader roots (so the existing module covers more files), or
 *   - kept as-is, plus additional "dummy" modules added to cover orphan files.
 *
 * Run as part of per-target build, before any entity is written: the spine consumes the [Result]
 * to know what source roots the main module should get and what dummy modules to add.
 */
// RC: replaces `JavaModuleToDummyJavaModulesTransformerHACK`; the merge/vote/restore logic is moved as-is,
// but the result no longer carries the `JavaModule` wrapper - the spine writes the entities directly
@ApiStatus.Internal
class DummyModuleSplitter(
  private val projectBasePath: Path,
  private val fileToTargets: File2TargetMap,
) {
  sealed interface Result

  data class MergedRoots(val mergedSourceRoots: List<SourceRootBuilder.ResolvedSourceRoot>) : Result

  data class DummyModulesToAdd(
    val originalSourceRoots: List<SourceRootBuilder.ResolvedSourceRoot>,
    val dummies: List<DummyModule>,
  ) : Result

  data class DummyModule(
    val name: String,
    val sourceRoot: SourceRootBuilder.ResolvedSourceRoot,
    val baseDir: Path,
  )

  fun split(
    baseDirectory: Path?,
    sourceRoots: List<SourceRootBuilder.ResolvedSourceRoot>,
  ): Result {
    val (relevantSourceRoots, irrelevantSourceRoots) = sourceRoots.partition { it.isRelevant() }
    val sourceRootsForParentDirs = calculateSourceRootsForParentDirs(relevantSourceRoots)
    val relevantSourceRootFiles = relevantSourceRoots.mapNotNullTo(mutableSetOf()) { it.sourcePath.findOrRefreshVirtualFile() }
    val finder = UnknownFileFinder(knownFiles = relevantSourceRootFiles, relevantExtensions = Constants.JVM_LANGUAGES_EXTENSIONS)
    val mergedSourceRootVotes = sourceRootsForParentDirs
      .restoreSourceRootFromPackagePrefix(finder, limit = baseDirectory)
      .preferShorterPrefix()

    if (BazelFeatureFlags.mergeSourceRoots) {
      val mergedSourceRoots =
        tryMergeSources(
          sourceRootFiles = relevantSourceRootFiles,
          mergeSourceRootVotes = mergedSourceRootVotes,
          sourceRootsForParentDirsVotes = sourceRootsForParentDirs,
          finder = finder,
        )
      if (mergedSourceRoots != null) {
        return MergedRoots(mergedSourceRoots = mergedSourceRoots + irrelevantSourceRoots)
      }
    }
    val dummySourceRoots =
      if (baseDirectory == null) {
        mergedSourceRootVotes
      }
      else {
        mergedSourceRootVotes.restoreSourceRootFromPackagePrefix(finder)
      }.keys.toList()
    val dummies = dummySourceRoots
      .map { root ->
        DummyModule(
          name = calculateDummyJavaModuleName(root.sourcePath, projectBasePath),
          // for some reason allowing dummy modules to be JAVA_TEST_SOURCE_ROOT_TYPE causes red code on https://github.com/bazelbuild/bazel
          sourceRoot = root.copy(rootType = JAVA_SOURCE_ROOT_TYPE),
          baseDir = root.sourcePath,
        )
      }.distinctBy { it.name }
    return DummyModulesToAdd(originalSourceRoots = sourceRoots, dummies = dummies)
  }

  private fun SourceRootBuilder.ResolvedSourceRoot.isRelevant(): Boolean =
    sourcePath.extension in Constants.JVM_LANGUAGES_EXTENSIONS || sourcePath.isDirectory()

  private fun tryMergeSources(
    sourceRootFiles: Set<VirtualFile>,
    mergeSourceRootVotes: Map<SourceRootBuilder.ResolvedSourceRoot, Int>,
    sourceRootsForParentDirsVotes: Map<SourceRootBuilder.ResolvedSourceRoot, Int>,
    finder: UnknownFileFinder,
  ): List<SourceRootBuilder.ResolvedSourceRoot>? {
    if (sourceRootFiles.any { it.isSharedBetweenSeveralTargets() }) {
      return null
    }
    return tryMergeSources(sourceRootFiles, mergeSourceRootVotes, finder)
           ?: tryMergeSources(sourceRootFiles, sourceRootsForParentDirsVotes, finder)
  }

  private fun tryMergeSources(
    originalSourceRoots: Set<VirtualFile>,
    mergeSourceRootVotes: Map<SourceRootBuilder.ResolvedSourceRoot, Int>,
    finder: UnknownFileFinder,
  ): List<SourceRootBuilder.ResolvedSourceRoot>? {
    val sourceRootsSortedByVotes: List<SourceRootBuilder.ResolvedSourceRoot> =
      mergeSourceRootVotes.entries
        // Prefer test roots over production roots (e.g., if some utility target is in the same package as a java_test).
        .sortedBy { (sourceRoot, _) -> sourceRoot.rootType != JAVA_TEST_SOURCE_ROOT_TYPE }
        // Sort by the number of segments in the source root path so ties go to the higher-in-tree root.
        .sortedBy { (sourceRoot, _) -> sourceRoot.sourcePath.nameCount }
        // Finally, by votes as the main criterion. "Reverse" sort order is intended:
        // see https://en.wikipedia.org/wiki/Sorting_algorithm#Stability
        .sortedByDescending { (_, votes) -> votes }
        .map { (sourceRoot, _) -> sourceRoot }
    val mergedSourceRoots = mutableListOf<SourceRootBuilder.ResolvedSourceRoot>()
    val mergedSourceRootFiles = mutableSetOf<VirtualFile>()
    val parentsOfMergedSourceRoots = mutableSetOf<VirtualFile>()

    for (sourceRoot in sourceRootsSortedByVotes) {
      val sourceRootFile = sourceRoot.sourcePath.findOrRefreshVirtualFile() ?: continue
      if (VfsUtilCore.isUnder(sourceRootFile, mergedSourceRootFiles)) continue
      if (sourceRootFile in parentsOfMergedSourceRoots) continue

      mergedSourceRoots.add(sourceRoot)
      mergedSourceRootFiles.add(sourceRootFile)
      parentsOfMergedSourceRoots.addAll(sourceRootFile.allAncestorsSequence())
    }

    if (originalSourceRoots.any { !VfsUtilCore.isUnder(it, mergedSourceRootFiles) }) return null

    if (mergedRootsCoverNewFiles(mergedSourceRootFiles, finder)) {
      return null
    }

    return mergedSourceRoots
  }

  /**
   * If after merging sources one source root becomes a parent of another, IDEA only considers the inner root
   * because of how the workspace model works. This can cause red code, e.g., on https://github.com/bazelbuild/bazel .
   */
  private fun VirtualFile.isSharedBetweenSeveralTargets(): Boolean = fileToTargets[toNioPath()].asSequence()
                                                                       .distinctBy { it.label to it.configuration }
                                                                       .count() > 1

  private fun mergedRootsCoverNewFiles(
    mergedRoots: Collection<VirtualFile>,
    finder: UnknownFileFinder,
  ): Boolean = mergedRoots.any { finder.containsUnknownMemo(it) }

  /**
   * Returns a map from a restored source root to the number of "votes" - the number of original source files
   * that "voted" for that root.
   */
  private fun calculateSourceRootsForParentDirs(
    sourceRoots: List<SourceRootBuilder.ResolvedSourceRoot>,
  ): Map<SourceRootBuilder.ResolvedSourceRoot, Int> =
    sourceRoots
      .asSequence()
      .filter { root -> !root.generated && root.sourcePath.startsWith(projectBasePath) }
      .mapNotNull { sourceRootForParentDir(it) }
      .groupingBy { it }
      .eachCount()
}

private fun sourceRootForParentDir(sourceRoot: SourceRootBuilder.ResolvedSourceRoot): SourceRootBuilder.ResolvedSourceRoot? {
  if (sourceRoot.sourcePath.isDirectory()) return null
  val sourceParent = sourceRoot.sourcePath.parent.pathString
  val sourceRootPath = Path(sourceParent)
  return SourceRootBuilder.ResolvedSourceRoot(
    sourcePath = sourceRootPath,
    generated = false,
    packagePrefix = sourceRoot.packagePrefix,
    rootType = sourceRoot.rootType,
  )
}

private fun Map<SourceRootBuilder.ResolvedSourceRoot, Int>.restoreSourceRootFromPackagePrefix(
  finder: UnknownFileFinder,
  limit: Path? = null,
): Map<SourceRootBuilder.ResolvedSourceRoot, Int> = this
  .map { (sourceRoot, votes) -> sourceRoot.restoreSourceRootFromPackagePrefix(finder, limit) to votes }
  .sumUpVotes()

private fun SourceRootBuilder.ResolvedSourceRoot.restoreSourceRootFromPackagePrefix(
  finder: UnknownFileFinder,
  limit: Path? = null,
): SourceRootBuilder.ResolvedSourceRoot {
  val segments = packagePrefix.split('.').toMutableList()
  var sourcePath: Path = this.sourcePath
  while (sourcePath != limit && segments.lastOrNull() == sourcePath.name) {
    sourcePath.parent ?: break
    if (sourcePath.siblingsContainUnknownRelevantFiles(finder)) break
    sourcePath = sourcePath.parent
    segments.removeLast()
  }
  return copy(sourcePath = sourcePath, packagePrefix = segments.joinToString("."))
}

private fun Path.siblingsContainUnknownRelevantFiles(finder: UnknownFileFinder): Boolean {
  val dir = parent.findOrRefreshVirtualFile() ?: return false
  val thisFile = this.findOrRefreshVirtualFile()
  return dir.children.any { it != thisFile && finder.containsUnknownMemo(it) }
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
  private val relevantExtensions: List<String>,
) {
  private val cache = HashMap<VirtualFile, Boolean>()

  fun containsUnknownMemo(file: VirtualFile): Boolean = cache.getOrPut(file) {
    if (!file.isDirectory) {
      return@getOrPut file.extension in relevantExtensions && file !in knownFiles
    }
    @Suppress("UnsafeVfsRecursion") // symlinks are ignored, so it's safe
    for (child in file.children ?: return@getOrPut false) {
      if (child.`is`(VFileProperty.SYMLINK)) {
        continue
      }
      if (child.isDirectory) {
        if (containsUnknownMemo(child)) {
          return@getOrPut true
        }
      }
      else if (child.extension in relevantExtensions && child !in knownFiles) {
        return@getOrPut true
      }
    }
    false
  }
}

private fun Path.findOrRefreshVirtualFile(): VirtualFile? {
  val file = this.findVirtualFile() ?: this.refreshAndFindVirtualFileOrDirectory()
  if (file == null) log.warn("Failed to find or refresh virtual file for path: $this!")
  return file
}

private fun VirtualFile.allAncestorsSequence(): Sequence<VirtualFile> = generateSequence(this) { it.parent }

private fun Iterable<Pair<SourceRootBuilder.ResolvedSourceRoot, Int>>.sumUpVotes(): Map<SourceRootBuilder.ResolvedSourceRoot, Int> {
  val result = mutableMapOf<SourceRootBuilder.ResolvedSourceRoot, Int>()
  for ((sourceRoot, votes) in this) {
    result[sourceRoot] = votes + result.getOrDefault(sourceRoot, 0)
  }
  return result
}

/**
 * Motivated by BAZEL-3050
 * It's only relevant when there is a mix of Kotlin and Java classes - like in the issue.
 * For example, when Kotlin sources are inside `com.example.foo` pacakge and Java sources are inside `foo` package, we prefer `foo`.
 * Without this fix, we would prefer `com.example.foo` which leads to red references to Java code from `foo` package.
 */
private fun Map<SourceRootBuilder.ResolvedSourceRoot, Int>.preferShorterPrefix(): Map<SourceRootBuilder.ResolvedSourceRoot, Int> {
  val grouped = entries.groupBy { (root, _) -> root.sourcePath to root.rootType }
  return grouped.flatMap { (_, group) ->
    if (group.size <= 1) return@flatMap group.map { it.toPair() }

    val sorted = group.sortedByDescending { (_, votes) -> votes }
    val first = sorted[0].key.packagePrefix
    val second = sorted[1].key.packagePrefix

    if (second.isEmpty() || first.endsWith(".$second")) {
      listOf(sorted[1].key to group.sumOf { (_, votes) -> votes })
    }
    else {
      group.map { it.toPair() }
    }
  }.toMap()
}

internal fun calculateDummyJavaModuleName(sourceRoot: Path, projectBasePath: Path): String {
  val absoluteSourceRoot = sourceRoot.toAbsolutePath().toString()
  val absoluteProjectBasePath = projectBasePath.toAbsolutePath().toString()
  return absoluteSourceRoot
    .substringAfter(absoluteProjectBasePath)
    .trim { it == File.separatorChar }
    .sanitizeName()
    .replace(File.separator, ".")
    .addIntelliJDummyPrefix()
    .shortenTargetPath()
}

private const val IJ_DUMMY_MODULE_PREFIX = "_aux.synthetic"

internal fun String.addIntelliJDummyPrefix(): String =
  if (isBlank()) IJ_DUMMY_MODULE_PREFIX else "$IJ_DUMMY_MODULE_PREFIX.$this"
