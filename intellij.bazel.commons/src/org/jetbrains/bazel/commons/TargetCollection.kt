package org.jetbrains.bazel.commons

import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants
import org.jetbrains.bazel.ignore.BazelIgnoreService
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.toPath
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.isDirectory
import kotlin.io.path.isSymbolicLink

private val log = logger<TargetCollection>()

/**
 * Utility class for working with collections of includable and excludable targets.
 * This replaces the old TargetsSpec class.
 */
@ApiStatus.Internal
data class TargetCollection(val values: List<Label>, val excludedValues: List<Label> = emptyList()) {
  companion object {
    fun fromExcludableList(targets: List<ExcludableValue<Label>>): TargetCollection {
      val included = mutableListOf<Label>()
      val excluded = mutableListOf<Label>()

      targets.forEach { excludableTarget ->
        if (excludableTarget.isIncluded()) {
          included.add(excludableTarget.value)
        } else {
          excluded.add(excludableTarget.value)
        }
      }

      return TargetCollection(included, excluded)
    }
  }
}

/**
 * Removes each excluded recursive pattern of the main workspace that has no `BUILD` file beneath its directory.
 *
 * Bazel rejects such a negative pattern with "no targets found beneath", and the pattern excludes nothing.
 * The walk skips the directories that [ignoreService] ignores and stops at the first `BUILD` file.
 * A nested module counts like any other directory, because Bazel includes its packages in the recursive pattern.
 * The walk does not follow symlinks, but Bazel does.
 * Thus a symlink to a directory, or a symlink with a `BUILD` file name, counts as a possible `BUILD` file.
 * A directory that the walk cannot read also keeps its pattern. See BAZEL-3586.
 */
@ApiStatus.Internal
suspend fun TargetCollection.dropExcludedPatternsWithoutPackages(
  workspaceRoot: Path,
  ignoreService: BazelIgnoreService,
): TargetCollection = withContext(Dispatchers.IO) {
  val patternsWithoutPackages = excludedValues
    .filter { it.isRecursive && it.isMainWorkspace }
    .filterTo(HashSet()) { !hasBuildFileBeneath(workspaceRoot.resolve(it.packagePath.toPath()), ignoreService, coroutineContext) }
  if (patternsWithoutPackages.isEmpty()) return@withContext this@dropExcludedPatternsWithoutPackages

  log.info("Dropping the excluded patterns without packages: $patternsWithoutPackages")
  copy(excludedValues = excludedValues.filterNot { it in patternsWithoutPackages })
}

private fun hasBuildFileBeneath(root: Path, ignoreService: BazelIgnoreService, context: CoroutineContext): Boolean {
  if (!root.isDirectory()) return false
  if (root.isSymbolicLink()) return true

  val queue = ArrayDeque<Path>().apply { add(root) }
  while (queue.isNotEmpty()) {
    context.ensureActive()
    val directory = queue.removeFirst()
    if (ignoreService.isIgnored(directory)) continue

    val entries: List<Pair<Path, BasicFileAttributes>> = try {
      Files.newDirectoryStream(directory).use { stream ->
        stream.map { it to Files.readAttributes(it, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS) }
      }
    }
    catch (e: IOException) {
      log.info("Keeping the excluded pattern for $root, because $directory cannot be read", e)
      return true
    }

    for ((path, attributes) in entries) {
      val isBuildFileName = path.fileName.toString() in Constants.BUILD_FILE_NAMES
      when {
        // With NOFOLLOW_LINKS, a symlink to a directory is not a directory, so check the symlink first.
        attributes.isSymbolicLink -> if (isBuildFileName || path.isDirectory()) return true
        attributes.isDirectory -> queue.add(path)
        isBuildFileName -> return true
      }
    }
  }
  return false
}
