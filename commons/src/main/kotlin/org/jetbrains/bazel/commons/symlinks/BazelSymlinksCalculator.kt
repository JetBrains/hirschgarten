package org.jetbrains.bazel.commons.symlinks

import org.jetbrains.bazel.commons.constants.Constants.WORKSPACE_FILE_NAMES
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.name

/**
 * Moved from [org.jetbrains.bazel.flow.open.exclude.BazelSymlinkExcludeService.getBazelSymlinksToExclude]
 */
object BazelSymlinksCalculator {
  fun calculateBazelSymlinksToExclude(workspaceRoot: Path, bazelSymlinksScanMaxDepth: Int): List<Path> {
    if (bazelSymlinksScanMaxDepth <= 0) return emptyList()
    // Don't scan non-Bazel projects for symlinks (because it can be quite slow).
    if (WORKSPACE_FILE_NAMES.none { workspaceFileName -> workspaceRoot.resolve(workspaceFileName).exists() }) return emptyList()

    val symlinksToExclude = mutableListOf<Path>()

    val bazelSymlinkEndings = listOf("bin", "out", "testlogs", workspaceRoot.name)

    val visitor =
      object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
          if (!attrs.isSymbolicLink) return FileVisitResult.CONTINUE
          if (bazelSymlinkEndings.none { file.name.endsWith(it) }) return FileVisitResult.CONTINUE
          // See https://bazel.build/remote/output-directories
          // This string used to be "execroot/_main", but for projects without Bzlmod the relevant path is actually "execroot/<my-project>"
          if (!file.toRealPath().invariantSeparatorsPathString.contains("/execroot/")) return FileVisitResult.CONTINUE
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
}
