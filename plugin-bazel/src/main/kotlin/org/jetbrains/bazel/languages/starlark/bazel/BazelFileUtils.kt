package org.jetbrains.bazel.languages.starlark.bazel

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.config.isBspProject
import org.jetbrains.bazel.config.rootDir
import java.io.File

private const val MAX_PATH_LENGTH = 25
private const val MIN_SEGMENTS_NUMBER = 1

object BazelFileUtils {
  fun getContainingDirectoryPresentablePath(project: Project, file: VirtualFile): String? {
    val relativeDir = getBazelFilePathFromRoot(project, file)?.removeSuffix(file.name)?.removeSuffix(File.separator)
    if (relativeDir.isNullOrBlank()) return null
    val presentablePath = truncatePathHead(relativeDir)
    return "${file.nameWithoutExtension} ($presentablePath)"
  }

  private fun getBazelFilePathFromRoot(project: Project, file: VirtualFile): String? =
    if (project.isBspProject) VfsUtilCore.getRelativePath(file, project.rootDir) else null

  private fun truncatePathHead(relativeDir: String): String {
    val segments = relativeDir.split(File.separator)
    var takenSegments = 0
    var length = 0
    val tailSegments =
      segments.takeLastWhile { segment ->
        length += segment.length
        takenSegments++
        length <= MAX_PATH_LENGTH || takenSegments <= MIN_SEGMENTS_NUMBER
      }
    val tailString = tailSegments.joinToString("/")
    return if (tailSegments.size == segments.size) "//$tailString" else "//.../$tailString"
  }
}
