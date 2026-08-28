package org.jetbrains.bazel.commons.symlinks

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.SystemInfoRt
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants.WORKSPACE_FILE_NAMES
import org.jetbrains.bazel.utils.isSymbolicLinkOrJunction
import org.jetbrains.bazel.utils.isWindowsJunction
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.io.path.readSymbolicLink
import kotlin.io.path.visitFileTree

@ApiStatus.Internal
object BazelSymlinksCalculator {
  fun calculateBazelSymlinksToExclude(workspaceRoot: Path, bazelSymlinksScanMaxDepth: Int): Set<Path> {
    if (bazelSymlinksScanMaxDepth <= 0) return emptySet()
    // Don't scan non-Bazel projects for symlinks (because it can be quite slow).
    if (WORKSPACE_FILE_NAMES.none { workspaceFileName -> workspaceRoot.resolve(workspaceFileName).exists() }) return emptySet()

    val symlinksToExclude = mutableSetOf<Path>()
    workspaceRoot.visitSymlinksAndJunctions(bazelSymlinksScanMaxDepth) {
      if (isBazelSymlink(workspaceRoot.name, it)) {
        symlinksToExclude.add(it)
      }
    }
    return symlinksToExclude
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

  fun isBazelSymlink(workspaceRootName: String, symlink: Path): Boolean {
    if (bazelSymlinkSuffixes(workspaceRootName).none { symlink.name.endsWith(suffix = it, ignoreCase = ignoreCase) }) {
      return false
    }
    val target = resolveSymlinkTarget(symlink)
    return when {
      target == null && symlink.isWindowsJunction -> {
        // on Windows, we cannot check the junction's target if it does not exist
        log.info("Unresolved junction $symlink excluded")
        true
      }
      target == null -> {
        log.info("Symlink $symlink not excluded - cannot resolve the symlink's target")
        false
      }
      // Compare with [ignoreCase] and not with Path.equals, which respects the case on macOS
      // although SystemInfoRt reports the macOS file system as case-insensitive.
      target.any { it.name.equals(EXEC_ROOT, ignoreCase = ignoreCase) } -> {
        // See https://bazel.build/remote/output-directories
        // This string used to be "execroot/_main", but for projects without Bzlmod the relevant path is actually "execroot/<my-project>"
        true
      }
      else -> {
        log.info("Symlink $symlink not excluded - symlink does not point to a Bazel output directory")
        false
      }
    }
  }

  fun resolveSymlinkTarget(symlink: Path): Path? = readSymbolicLinkTarget(symlink) ?: readJunctionTarget(symlink)

  private fun readSymbolicLinkTarget(symlink: Path): Path? =
    try {
      symlink.resolveSibling(symlink.readSymbolicLink()).normalize()
    }
    catch (_: IOException) {
      null
    }

  /**
   * Reads the target of a Windows junction.
   *
   * [readSymbolicLink] rejects a junction, because a junction carries the `IO_REPARSE_TAG_MOUNT_POINT` reparse tag
   * and not `IO_REPARSE_TAG_SYMLINK`. [Path.toRealPath] resolves a junction, but it needs the target to exist.
   */
  private fun readJunctionTarget(junction: Path): Path? {
    if (!junction.isWindowsJunction) return null
    return try {
      junction.toRealPath()
    }
    catch (_: IOException) {
      null
    }
  }

  fun isBazelSymlink(workspaceRootName: String, file: VirtualFile): Boolean =
    file.toNioPathOrNull()?.let { isBazelSymlink(workspaceRootName, it) } == true

  private fun bazelSymlinkSuffixes(workspaceRootName: String): List<String> = listOf("bin", "out", "testlogs", workspaceRootName)

  private const val EXEC_ROOT = "execroot"

  private val ignoreCase = !SystemInfoRt.isFileSystemCaseSensitive

  private val log = logger<BazelSymlinksCalculator>()
}
