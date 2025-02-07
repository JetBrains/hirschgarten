package org.jetbrains.bazel.commons.symlinks

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.name

/**
 * Moved from [org.jetbrains.bazel.flow.open.exclude.BazelSymlinkExcludeService.getBazelSymlinksToExclude]
 */
object BazelSymlinksCalculator {
  fun calculateBazelSymlinksToExclude(workspaceRoot: Path, bazelSymlinksScanMaxDepth: Int): List<Path> {
    val symlinksToExclude = kotlin.collections.mutableListOf<Path>()

    val bazelSymlinkEndings = listOf("bin", "out", "testlogs", workspaceRoot.name)

    val visitor =
      object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes?): FileVisitResult {
          if (bazelSymlinkEndings.none { file.name.endsWith(it) }) return FileVisitResult.CONTINUE
          val resolved = file.toRealPath()
          if (resolved == file) return FileVisitResult.CONTINUE
          // See https://bazel.build/remote/output-directories
          if (!resolved.invariantSeparatorsPathString.contains("execroot/_main")) return FileVisitResult.SKIP_SUBTREE
          symlinksToExclude.add(file)
          return FileVisitResult.SKIP_SUBTREE
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
