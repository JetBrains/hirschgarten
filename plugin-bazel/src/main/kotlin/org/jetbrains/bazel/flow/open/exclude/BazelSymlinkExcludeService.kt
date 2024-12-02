package org.jetbrains.bazel.flow.open.exclude

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.jetbrains.bazel.config.BazelFeatureFlags
import org.jetbrains.plugins.bsp.config.rootDir
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.name

@Service(Service.Level.PROJECT)
class BazelSymlinkExcludeService(private val project: Project) : DumbAware {
  private var symlinksToExclude: List<Path>? = null

  @Synchronized
  fun getBazelSymlinksToExclude(bazelWorkspace: Path = project.rootDir.toNioPath()): List<Path> {
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
    Files.walkFileTree(bazelWorkspace, emptySet(), BazelFeatureFlags.symlinkScanMaxDepth, visitor)

    if (symlinksToExclude.isNotEmpty()) {
      this.symlinksToExclude = symlinksToExclude
    }
    return symlinksToExclude
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project): BazelSymlinkExcludeService = project.service()
  }
}
