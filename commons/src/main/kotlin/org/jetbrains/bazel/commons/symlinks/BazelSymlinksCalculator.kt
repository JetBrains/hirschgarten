package org.jetbrains.bazel.commons.symlinks

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

object BazelSymlinksCalculator {
  fun calculateBazelSymlinksToExclude(workspaceRoot: Path, bazelSymlinksScanMaxDepth: Int): Set<Path> {
    if (bazelSymlinksScanMaxDepth <= 0) return emptySet()
    // Don't scan non-Bazel projects for symlinks (because it can be quite slow).
    if (WORKSPACE_FILE_NAMES.none { workspaceFileName -> workspaceRoot.resolve(workspaceFileName).exists() }) return emptySet()

    val symlinksToExclude = mutableSetOf<Path>()

    val visitor =
      object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
          if (!attrs.isSymbolicLink) return FileVisitResult.CONTINUE
          if (!isBazelSymlink(workspaceRoot.name, file)) return FileVisitResult.CONTINUE
          symlinksToExclude.add(file)
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

  fun isBazelSymlink(workspaceRootName: String, file: Path): Boolean {
    val bazelSymlinkEndings = listOf("bin", "out", "testlogs", workspaceRootName)
    if (bazelSymlinkEndings.none { file.name.endsWith(it) }) return false

    val realPath =
      try {
        file.toRealPath()
      } catch (_: IOException) {
        // Symlink may have a broken target after bazel clean
        return false
      }

    // See https://bazel.build/remote/output-directories
    // This string used to be "execroot/_main", but for projects without Bzlmod the relevant path is actually "execroot/<my-project>"
    return realPath.invariantSeparatorsPathString.contains("/execroot/")
  }
}
