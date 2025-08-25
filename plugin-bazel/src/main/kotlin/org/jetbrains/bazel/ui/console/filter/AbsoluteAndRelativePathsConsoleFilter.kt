package org.jetbrains.bazel.ui.console.filter

import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir

private const val PATH_GROUP_ID = "path"
private const val LINE_GROUP_ID = "line"
private const val COLUMN_GROUP_ID = "column"

private val BspPathRegex =
  """
        (^|\W)                             # start of line or non-word character
        (?<$PATH_GROUP_ID>[0-9 a-z_A-Z-/.]+)   # match the path, even relative
        (?::(?<$LINE_GROUP_ID>[0-9]+))?        # optional line number
        (?::(?<$COLUMN_GROUP_ID>[0-9]+))?      # optional column number
    """.toRegex(setOf(RegexOption.COMMENTS, RegexOption.MULTILINE))

/**
 * A better version of [com.intellij.execution.filters.RegexpFilter] which supports relative paths as well
 *
 * It tries to match:
 *  - absolute path if exists
 *  - relative path if exists and is not in the root of the project, why?
 *    Because then we can get a lot of false positives with just normal names in the console
 *    e.g. word "Build" could be matched to a BUILD file in the project root.
 *    So to avoid such cases, we do not mach anything in the root of the project -
 *    it's highly unlikely to have a non-path word in the console which has slash in it
 *    and maps to an existing file
 */
class AbsoluteAndRelativePathsConsoleFilter(private val project: Project) : Filter {
  override fun applyFilter(line: String, entireLength: Int): Filter.Result {
    val resultItems =
      BspPathRegex
        .findAll(line)
        .mapNotNull { it.toFilterResultOrNull(line, entireLength) }
        .toList()
    return Filter.Result(resultItems)
  }

  private fun MatchResult.toFilterResultOrNull(line: String, entireLength: Int): Filter.Result? {
    val pathGroup = groups[PATH_GROUP_ID] ?: return null
    val virtualFile = pathGroup.value.toVirtualFileInTheProject() ?: return null

    val info = calculateInfo(virtualFile)
    val highlightStartOffset = calculateStartOffset(entireLength, line.length, pathGroup)
    val highlightEndOffset = calculateEndOffset(highlightStartOffset, pathGroup.value.length)

    return Filter.Result(highlightStartOffset, highlightEndOffset, info)
  }

  private fun String.toVirtualFileInTheProject(): VirtualFile? {
    if ('/' !in this) return null
    if (trim() == "/") return null

    return LocalFileSystem
      .getInstance()
      .findFileByPath(toAbsolutePath())
      ?.canonicalFile
      ?.takeIf { it.exists() }
  }

  private fun String.toAbsolutePath(): String = if (startsWith("/")) this else "${project.rootDir.path}/$this"

  private fun MatchResult.calculateInfo(virtualFile: VirtualFile): OpenFileHyperlinkInfo {
    val lineNumber = groups[LINE_GROUP_ID]?.value?.toIntOrNull()?.dec() ?: 0
    val columnNumber = groups[COLUMN_GROUP_ID]?.value?.toIntOrNull()?.dec() ?: 0

    return OpenFileHyperlinkInfo(project, virtualFile, lineNumber, columnNumber)
  }

  private fun calculateStartOffset(
    entireLength: Int,
    lineLength: Int,
    pathGroup: MatchGroup,
  ): Int = entireLength - lineLength + pathGroup.range.first

  private fun MatchResult.calculateEndOffset(highlightStartOffset: Int, rawPathLength: Int): Int {
    val lineGroupLengthWithColon = groups[LINE_GROUP_ID]?.value?.length?.inc() ?: 0
    val columnGroupLengthWithColon = groups[COLUMN_GROUP_ID]?.value?.length?.inc() ?: 0

    return highlightStartOffset + rawPathLength + lineGroupLengthWithColon + columnGroupLengthWithColon
  }
}

class AbsoluteAndRelativePathsConsoleFilterProvider : ConsoleFilterProvider {
  override fun getDefaultFilters(project: Project): Array<out Filter> =
    if (project.isBazelProject) arrayOf(AbsoluteAndRelativePathsConsoleFilter(project)) else emptyArray()
}
