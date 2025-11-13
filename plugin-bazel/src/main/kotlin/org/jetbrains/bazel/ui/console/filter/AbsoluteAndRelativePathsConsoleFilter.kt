package org.jetbrains.bazel.ui.console.filter

import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.toNioPathOrNull
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.utils.findCanonicalVirtualFileThatExists

private const val PATH_GROUP_ID = "path"
private const val LINE_GROUP_ID = "line"
private const val COLUMN_GROUP_ID = "column"

private val PathRegex = Regex(
  pattern = """
        (^|\W)                                  # start of line or non-word character
        (?<$PATH_GROUP_ID>[0-9 a-z_A-Z-/.+]+)   # match the path, even relative
    """,
  options = setOf(RegexOption.COMMENTS, RegexOption.MULTILINE),
)

private val PositionRegex = Regex(":(?<$LINE_GROUP_ID>\\d+):(?<$COLUMN_GROUP_ID>\\d+)")
private val IntellijPositionRegex = Regex(" \\((?<$LINE_GROUP_ID>\\d+):(?<$COLUMN_GROUP_ID>\\d+)\\)")

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
      PathRegex
        .findAll(line)
        .mapNotNull { it.toFilterResultOrNull(line, entireLength) }
        .toList()
    return Filter.Result(resultItems)
  }

  private fun MatchResult.toFilterResultOrNull(line: String, entireLength: Int): Filter.Result? {
    val pathGroup = groups[PATH_GROUP_ID] ?: return null
    val virtualFile = pathGroup.value.toVirtualFileInTheProject() ?: return null
    val positionRegexResult = PositionRegex.matchAt(line, range.last + 1)
      ?: IntellijPositionRegex.matchAt(line, range.last + 1)
    val info = calculateInfo(virtualFile, positionRegexResult)
    val highlightStartOffset = calculateStartOffset(entireLength, line.length, pathGroup)
    val highlightEndOffset = calculateEndOffset(highlightStartOffset, pathGroup, positionRegexResult)
    return Filter.Result(highlightStartOffset, highlightEndOffset, info)
  }

  private fun String.toVirtualFileInTheProject(): VirtualFile? {
    if ('/' !in this) return null
    if (trim() == "/") return null
    return toAbsolutePath()
      .toNioPathOrNull()
      ?.findCanonicalVirtualFileThatExists()
  }

  private fun String.toAbsolutePath() = if (startsWith("/")) this else "${project.rootDir.path}/$this"

  private fun calculateInfo(virtualFile: VirtualFile, positionRegexResult: MatchResult?): OpenFileHyperlinkInfo {
    val lineNumber = positionRegexResult?.groups[LINE_GROUP_ID]?.value?.toIntOrNull()?.dec() ?: 0
    val columnNumber = positionRegexResult?.groups[COLUMN_GROUP_ID]?.value?.toIntOrNull()?.dec() ?: 0
    return OpenFileHyperlinkInfo(project, virtualFile, lineNumber, columnNumber)
  }

  private fun calculateStartOffset(
    entireLength: Int,
    lineLength: Int,
    pathGroup: MatchGroup,
  ): Int = entireLength - lineLength + pathGroup.range.first

  private fun calculateEndOffset(
    highlightStartOffset: Int,
    pathGroup: MatchGroup,
    locationRegexResult: MatchResult?,
  ): Int = highlightStartOffset + pathGroup.value.length + (locationRegexResult?.value?.length ?: 0)
}

class AbsoluteAndRelativePathsConsoleFilterProvider : ConsoleFilterProvider {
  override fun getDefaultFilters(project: Project): Array<out Filter> =
    if (project.isBazelProject) arrayOf(AbsoluteAndRelativePathsConsoleFilter(project)) else emptyArray()
}
