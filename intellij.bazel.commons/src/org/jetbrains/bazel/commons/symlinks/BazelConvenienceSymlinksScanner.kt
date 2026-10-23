package org.jetbrains.bazel.commons.symlinks

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants.WORKSPACE_FILE_NAMES
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.bazel.utils.isSymbolicLinkOrJunction
import org.jetbrains.bazel.utils.isWindowsJunction
import org.jetbrains.bazel.utils.readSymbolicLinkTarget
import org.jetbrains.bazel.utils.toRealPathOrNull
import java.nio.file.FileVisitResult
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readSymbolicLink
import kotlin.io.path.visitFileTree

private val log = logger<BazelConvenienceSymlinksScanner>()

@ApiStatus.Internal
class BazelConvenienceSymlinksScanner(
  val workspaceRoot: Path,
  private val maxDepth: Int = BazelFeatureFlags.symlinkScanMaxDepth,
) {
  private val ignoreCase = !SystemInfoRt.isFileSystemCaseSensitive

  private val bazelSymlinkSuffixes = listOf("bin", "out", "testlogs", workspaceRoot.name)

  /**
   * Scans for all Bazel convenience symlinks in the [workspaceRoot] as deep as [maxDepth].
   */
  fun scan(): Set<Path> {
    if (maxDepth <= 0) return emptySet()
    // Don't scan non-Bazel projects for symlinks (because it can be quite slow).
    if (WORKSPACE_FILE_NAMES.none { workspaceFileName -> workspaceRoot.resolve(workspaceFileName).exists() }) return emptySet()
    val symlinks = mutableSetOf<Path>()
    workspaceRoot.visitSymlinksAndJunctions(maxDepth) {
      if (isBazelSymlink(it)) {
        symlinks.add(it)
      }
    }
    return symlinks
  }

  /**
   * Whether [path] lies under a Bazel convenience symlink, looking as deep as [scan] does.
   */
  fun isUnderBazelSymlink(path: Path): Boolean = path
    .ancestorsWithinScanDepth()
    .any { it.isSymbolicLinkOrJunction && isBazelSymlink(it) }

  fun isBazelSymlink(file: VirtualFile): Boolean = file
    .toNioPathOrNull()
    ?.let(::isBazelSymlink) == true

  /**
   * Whether [path] is a Bazel convenience symlink. It checks the name and the target only.
   * Unlike [scan] and [isUnderBazelSymlink], it does not limit the depth.
   */
  fun isBazelSymlink(path: Path): Boolean {
    if (bazelSymlinkSuffixes.none { path.name.endsWith(suffix = it, ignoreCase = ignoreCase) }) return false
    val target = resolveSymlinkTarget(path)
    return when {
      target == null && path.isWindowsJunction -> {
        // on Windows, we cannot check the junction's target if it does not exist
        log.info("Unresolved junction $path excluded")
        true
      }
      target == null -> {
        log.info("Symlink $path not excluded - cannot resolve the symlink's target")
        false
      }
      // Compare with [ignoreCase] and not with Path.equals, which respects the case on macOS
      // although SystemInfoRt reports the macOS file system as case-insensitive.
      target.any { it.name.equals("execroot", ignoreCase = ignoreCase) } -> {
        // See https://bazel.build/remote/output-directories
        // This string used to be "execroot/_main", but for projects without Bzlmod the relevant path is actually "execroot/<my-project>"
        true
      }
      else -> {
        log.info("Symlink $path not excluded - symlink does not point to a Bazel output directory")
        false
      }
    }
  }

  /**
   * The ancestors of this path below the workspace root, from the shallowest one, at most [maxDepth] of them.
   */
  private fun Path.ancestorsWithinScanDepth(): Sequence<Path> {
    if (!startsWith(workspaceRoot) || nameCount == workspaceRoot.nameCount) return emptySequence()
    val relativePath = subpath(workspaceRoot.nameCount, nameCount)
    val segmentsRange = 1..minOf(relativePath.nameCount, maxDepth)
    return segmentsRange
      .asSequence()
      .map { workspaceRoot.resolve(relativePath.subpath(0, it)) }
  }

  companion object {
    fun resolveSymlinkTarget(symlink: Path): Path? = symlink.readSymbolicLinkTarget() ?: symlink.readJunctionTarget()
  }
}

private fun Path.visitSymlinksAndJunctions(
  maxDepth: Int,
  block: (Path) -> Unit
) = visitFileTree(maxDepth = maxDepth) {
  onVisitFileFailed { file, exception ->
    // The walk opens a junction as a directory, which fails when the junction's target is missing.
    when {
      file != this@visitSymlinksAndJunctions && file.isWindowsJunction -> block(file)
      else -> log.warn("Failed to visit file $file", exception)
    }
    FileVisitResult.SKIP_SUBTREE
  }
  onPreVisitDirectory { directory, attributes ->
    if (directory == this@visitSymlinksAndJunctions || !attributes.isSymbolicLinkOrJunction) FileVisitResult.CONTINUE
    else {
      block(directory)
      // directory + isSymbolicLinkOrJunction means it's a junction, and we need to skip the subtree manually
      FileVisitResult.SKIP_SUBTREE
    }
  }
  onVisitFile { file, attributes ->
    if (attributes.isSymbolicLinkOrJunction) block(file)
    FileVisitResult.CONTINUE
  }
}

/**
 * Reads the target of a Windows junction.
 *
 * [readSymbolicLink] rejects a junction, because a junction carries the `IO_REPARSE_TAG_MOUNT_POINT` reparse tag
 * and not `IO_REPARSE_TAG_SYMLINK`. [Path.toRealPath] resolves a junction, but it needs the target to exist.
 */
private fun Path.readJunctionTarget(): Path? = if (isWindowsJunction) toRealPathOrNull() else null
