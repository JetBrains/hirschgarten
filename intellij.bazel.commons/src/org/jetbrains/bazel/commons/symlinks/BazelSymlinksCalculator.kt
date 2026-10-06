package org.jetbrains.bazel.commons.symlinks

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.commons.constants.Constants.WORKSPACE_FILE_NAMES
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.name
import kotlin.io.path.readSymbolicLink

@ApiStatus.Internal
object BazelSymlinksCalculator {
  fun calculateBazelSymlinksToExclude(workspaceRoot: Path, bazelSymlinksScanMaxDepth: Int): Set<Path> {
    if (bazelSymlinksScanMaxDepth <= 0) return emptySet()
    // Don't scan non-Bazel projects for symlinks (because it can be quite slow).
    if (WORKSPACE_FILE_NAMES.none { workspaceFileName -> workspaceRoot.resolve(workspaceFileName).exists() }) return emptySet()

    val symlinksToExclude = mutableSetOf<Path>()

    val visitor =
      object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
          if (attrs.isSymbolicLink && isBazelSymlink(workspaceRoot.name, file)) {
            symlinksToExclude.add(file)
          }
          return FileVisitResult.CONTINUE
        }
      }

    Files.walkFileTree(
      workspaceRoot,
      emptySet(),
      bazelSymlinksScanMaxDepth,
      visitor,
    )
    return symlinksToExclude
  }

  fun isBazelSymlink(workspaceRootName: String, symlink: Path): Boolean {
    if (bazelSymlinkSuffixes(workspaceRootName).none { symlink.name.endsWith(it) }) {
      return false
    }

    val target = resolveSymlinkTarget(symlink)
    if (target == null) {
      log.info("Symlink $symlink not excluded - cannot resolve the symlink's target")
      return false
    }

    // See https://bazel.build/remote/output-directories
    // This string used to be "execroot/_main", but for projects without Bzlmod the relevant path is actually "execroot/<my-project>"
    if (target.invariantSeparatorsPathString.contains("/execroot/")) {
      return true
    } else {
      log.info("Symlink $symlink not excluded - symlink does not point to a Bazel output directory")
      return false
    }
  }

  fun resolveSymlinkTarget(symlink: Path): Path? {
    val target =
      try {
        symlink.readSymbolicLink()
      } catch (_: IOException) {
        return null
      }
    return symlink.resolveSibling(target).normalize()
  }

  fun isBazelSymlink(workspaceRootName: String, file: VirtualFile): Boolean =
    file.toNioPathOrNull()?.let { isBazelSymlink(workspaceRootName, it) } == true

  private fun bazelSymlinkSuffixes(workspaceRootName: String): List<String> = listOf("bin", "out", "testlogs", workspaceRootName)

  private val log = logger<BazelSymlinksCalculator>()
}
