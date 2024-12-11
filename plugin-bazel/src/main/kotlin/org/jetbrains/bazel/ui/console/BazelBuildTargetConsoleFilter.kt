package org.jetbrains.bazel.ui.console

import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.plugins.bsp.config.rootDir
import kotlin.io.path.Path

class BazelBuildTargetConsoleFilter(private val project: Project) : Filter {
  private val bazelTargetRegex =
    """(^|\W)
        (?<highlightGroup>
        ((@|@@)(?<externalRepoGroup>[a-zA-Z0-9!%\-^_"&'()*+,;<=>?\[\]{|}~/.\#]*))? #@ or @@ with potential external repo names
        //(?<pathGroup>[a-zA-Z0-9!%\-@^_"&'()*+,;<=>?\[\]{|}~/.\#]*):      #path
        ([a-zA-Z0-9!%\-@^_"&'()*+,;<=>?\[\]{|}~/.\#]+)         #target
        )
    """.trimMargin()
      .toRegex(setOf(RegexOption.COMMENTS, RegexOption.MULTILINE))

  override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
    val results = bazelTargetRegex.findAll(line).mapNotNull { it.toFilterResultOrNull(line, entireLength) }
    return Filter.Result(results.toList())
  }

  private fun MatchResult.toFilterResultOrNull(line: String, entireLength: Int): Filter.Result? {
    val highlightGroup = groups["highlightGroup"] ?: return null
    val pathGroup = groups["pathGroup"] ?: return null
    val externalRepoGroup = groups["externalRepoGroup"]
    if (externalRepoGroup != null && externalRepoGroup.value.isNotEmpty()) {
      // skip targets in external repo
      return null
    }
    val virtualFile = pathGroup.value.toBazelFileInProject() ?: return null

    val hyperLinkInfo = OpenFileHyperlinkInfo(project, virtualFile, 0, 0)

    val highlightStartOffset = entireLength - line.length + highlightGroup.range.first
    val highlightEndOffset = entireLength - line.length + highlightGroup.range.last + 1

    return Filter.Result(highlightStartOffset, highlightEndOffset, hyperLinkInfo)
  }

  private fun String.toBazelFileInProject(): VirtualFile? {
    if (trim() == "/") return null
    return toVirtualFile(toAbsolutePath() + "/BUILD.bazel") ?: return toVirtualFile(toAbsolutePath() + "/BUILD")
  }

  private fun String.toAbsolutePath(): String = if (startsWith("/")) this else "${project.rootDir.path}/$this"

  private fun toVirtualFile(path: String): VirtualFile? = VirtualFileManager.getInstance().findFileByNioPath(Path(path))
}

class BazelBuildTargetConsoleFilterProvider : ConsoleFilterProvider {
  override fun getDefaultFilters(project: Project): Array<out Filter> =
    if (project.isBazelProject) arrayOf(BazelBuildTargetConsoleFilter(project)) else emptyArray()
}
