package org.jetbrains.bazel.settings

import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.plugins.bsp.config.rootDir

class BazelConsoleFilter(private val project: Project) : Filter {
  private val bazelTargetRegex =
    """(^|\W)
        (
        ((@|@@)([a-zA-Z0-9!%\-^_"&'()*+,;<=>?\[\]{|}~/.\#]*))? #@ or @@ with potential external repo names
        //([a-zA-Z0-9!%\-@^_"&'()*+,;<=>?\[\]{|}~/.\#]*):      #path
        ([a-zA-Z0-9!%\-@^_"&'()*+,;<=>?\[\]{|}~/.\#]+)         #target
        )
    """.trimMargin()
      .toRegex(setOf(RegexOption.COMMENTS, RegexOption.MULTILINE))

  override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
    val results = bazelTargetRegex.findAll(line).mapNotNull { it.toFilterResultOrNull(line, entireLength) }
    return Filter.Result(results.toList())
  }

  private fun MatchResult.toFilterResultOrNull(line: String, entireLength: Int): Filter.Result? {
    val highlightGroup = groups[2] ?: return null
    val pathGroup = groups[6] ?: return null
    val externalRepoGroup = groups[5]
    if (externalRepoGroup != null && externalRepoGroup.value.isNotEmpty()) {
      // skip targets in external repo
      return null
    }
    val virtualFile = pathGroup.value.toVirtualFileInTheProject() ?: return null

    val hyperLinkInfo = OpenFileHyperlinkInfo(project, virtualFile, 0, 0)

    val highlightStartOffset = entireLength - line.length + highlightGroup.range.first
    val highlightEndOffset = entireLength - line.length + highlightGroup.range.last + 1

    return Filter.Result(highlightStartOffset, highlightEndOffset, hyperLinkInfo)
  }

  private fun String.toVirtualFileInTheProject(): VirtualFile? {
    if (trim() == "/") return null
    val buildFile = LocalFileSystem.getInstance().findFileByPathIfCached(toAbsolutePath() + "/BUILD.bazel")?.takeIf { it.exists() }
    if (buildFile == null) {
      return LocalFileSystem.getInstance().findFileByPathIfCached(toAbsolutePath() + "/BUILD")?.takeIf { it.exists() }
    }
    return buildFile
  }

  private fun String.toAbsolutePath(): String = if (startsWith("/")) this else "${project.rootDir.path}/$this"
}

class BazelConsoleFilterProvider : ConsoleFilterProvider {
  override fun getDefaultFilters(project: Project): Array<out Filter> =
    if (project.isBazelProject) arrayOf(BazelConsoleFilter(project)) else emptyArray()
}
