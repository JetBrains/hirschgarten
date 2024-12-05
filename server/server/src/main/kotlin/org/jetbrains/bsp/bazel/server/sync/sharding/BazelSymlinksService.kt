package org.jetbrains.bsp.bazel.server.sync.sharding

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.name

// taken from org.jetbrains.bazel.flow.open.exclude.BazelSymlinkExcludeService
// TODO: merge them into 1
object BazelSymlinksService {
  private var symlinksToExclude: List<Path>? = null

  @Synchronized
  fun getBazelSymlinksToExclude(bazelWorkspace: Path): List<Path> {
    this.symlinksToExclude?.let { return it }
    val symlinksToExclude = mutableListOf<Path>()

    val bazelSymlinkEndings = listOf("bin", "out", "testlogs", bazelWorkspace.name)

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
    Files.walkFileTree(bazelWorkspace, emptySet(), 1, visitor)

    if (symlinksToExclude.isNotEmpty()) {
      this.symlinksToExclude = symlinksToExclude
    }
    return symlinksToExclude
  }
}
