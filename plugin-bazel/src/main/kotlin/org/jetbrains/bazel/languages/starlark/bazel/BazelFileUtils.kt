package org.jetbrains.bazel.languages.starlark.bazel

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.toNioPathOrNull
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.languages.starlark.repomapping.calculateLabel
import org.jetbrains.bazel.languages.starlark.repomapping.toApparentLabelOrThis

private const val MAX_PATH_LENGTH = 25
private const val MIN_SEGMENTS_NUMBER = 1

object BazelFileUtils {
  fun getContainingDirectoryPresentablePath(project: Project, file: VirtualFile): String? {
    val buildFile = file.toNioPathOrNull() ?: return null
    val relativeDir = calculateLabel(project, buildFile)?.toApparentLabelOrThis(project) as? ResolvedLabel ?: return null
    if (relativeDir.repo is Main && relativeDir.packagePath.pathSegments.isEmpty()) return null
    val presentablePath = truncatePathHead(relativeDir)
    return "${file.nameWithoutExtension} ($presentablePath)"
  }

  private fun truncatePathHead(relativeDir: ResolvedLabel): String {
    val segments = relativeDir.packagePath.pathSegments
    var takenSegments = 0
    var length = 0
    val tailSegments =
      segments.takeLastWhile { segment ->
        length += segment.length
        takenSegments++
        length <= MAX_PATH_LENGTH || takenSegments <= MIN_SEGMENTS_NUMBER
      }
    val tailString = tailSegments.joinToString("/")
    val repo = relativeDir.repo
    val repoString = if (repo is Main) "" else repo.toString()
    return if (tailSegments.size == segments.size) "$repoString//$tailString" else "$repoString//.../$tailString"
  }
}
