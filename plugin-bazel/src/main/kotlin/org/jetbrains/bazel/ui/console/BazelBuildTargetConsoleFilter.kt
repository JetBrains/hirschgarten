package org.jetbrains.bazel.ui.console

import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDirectory
import com.intellij.openapi.vfs.findFile
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.plugins.bsp.config.rootDir

class BazelBuildTargetConsoleFilter(private val project: Project) : Filter {
  private val highlightGroupName = "highlightGroup"
  private val externalRepoGroupName = "externalRepoGroup"
  private val pathGroupName = "pathGroup"
  
  // The set of characters allowed in labels is taken from https://bazel.build/concepts/labels#target-names 
  private val bazelTargetRegex =
    """(^|\W)
        (?<$highlightGroupName>
        ((@|@@)(?<$externalRepoGroupName>[a-zA-Z0-9!%\-^_"&'()*+,;<=>?\[\]{|}~/.\#]*))? #@ or @@ with potential external repo names
        //(?<$pathGroupName>[a-zA-Z0-9!%\-@^_"&'()*+,;<=>?\[\]{|}~/.\#]*):      #path
        ([a-zA-Z0-9!%\-@^_"&'()*+,;<=>?\[\]{|}~/.\#]+)         #target
        )
    """.trimMargin()
      .toRegex(setOf(RegexOption.COMMENTS, RegexOption.MULTILINE))

  override fun applyFilter(line: String, entireLength: Int): Filter.Result? {
    val results = bazelTargetRegex.findAll(line).mapNotNull { it.toFilterResultOrNull(line, entireLength) }
    return Filter.Result(results.toList())
  }

  private fun MatchResult.toFilterResultOrNull(line: String, entireLength: Int): Filter.Result? {
    val highlightGroup = groups[highlightGroupName] ?: return null
    val pathGroup = groups[pathGroupName] ?: return null
    val externalRepoGroup = groups[externalRepoGroupName]
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

  private fun String.toBazelFileInProject(): VirtualFile? =
    project.rootDir.findDirectory(this)?.findFile("BUILD.bazel")
      ?: project.rootDir.findDirectory(this)?.findFile("BUILD")
}

class BazelBuildTargetConsoleFilterProvider : ConsoleFilterProvider {
  override fun getDefaultFilters(project: Project): Array<out Filter> =
    if (project.isBazelProject) arrayOf(BazelBuildTargetConsoleFilter(project)) else emptyArray()
}
