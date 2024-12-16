package org.jetbrains.bazel.ui.console

import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findDirectory
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.util.descendantsOfType
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.bazel.BazelLabel
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.references.BUILD_FILE_NAMES
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.plugins.bsp.config.rootDir

class BazelBuildTargetConsoleFilter(private val project: Project) : Filter {
  private val highlightGroupName = "highlightGroup"
  private val externalRepoGroupName = "externalRepoGroup"
  private val pathGroupName = "pathGroup"
  private val targetName = "targetName"

  // The set of characters allowed in labels is taken from https://bazel.build/concepts/labels#target-names
  private val bazelTargetRegex =
    """(^|\W)
        (?<$highlightGroupName>
        ((@|@@)(?<$externalRepoGroupName>[a-zA-Z0-9!%\-^_"&'()*+,;<=>?\[\]{|}~/.\#]*))? #@ or @@ with potential external repo names
        //(?<$pathGroupName>[a-zA-Z0-9!%\-@^_"&'()*+,;<=>?\[\]{|}~/.\#]*):      #path
        (?<$targetName>[a-zA-Z0-9!%\-@^_"&'()*+,;<=>?\[\]{|}~/.\#]+)         #target
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
    val target = groups[targetName] ?: return null
    if (externalRepoGroup != null && externalRepoGroup.value.isNotEmpty()) {
      // skip targets in external repo
      return null
    }
    val virtualFile = pathGroup.value.toBazelFileInProject() ?: return null
    val hyperLinkInfo =
      runReadAction {
        val psiElement =
          virtualFile
            .toPsiFile(project)
            ?.descendantsOfType<StarlarkNamedArgumentExpression>()
            ?.filter { it.isNameArgument() }
            ?.firstOrNull {
              it.getArgumentStringValue()?.let { it1 -> BazelLabel.ofString(it1).targetName } == target.value
            }
        OpenFileHyperlinkInfo(project, virtualFile, psiElement?.getLineNumber() ?: 0, 0)
      }

    val highlightStartOffset = entireLength - line.length + highlightGroup.range.first
    val highlightEndOffset = entireLength - line.length + highlightGroup.range.last + 1

    return Filter.Result(highlightStartOffset, highlightEndOffset, hyperLinkInfo)
  }

  private fun String.toBazelFileInProject(): VirtualFile? {
    for (fileName in BUILD_FILE_NAMES) {
      val buildFile = project.rootDir.findDirectory(this)?.findFile(fileName)
      if (buildFile != null) {
        return buildFile
      }
    }
    return null
  }
}

class BazelBuildTargetConsoleFilterProvider : ConsoleFilterProvider {
  override fun getDefaultFilters(project: Project): Array<out Filter> =
    if (project.isBazelProject) arrayOf(BazelBuildTargetConsoleFilter(project)) else emptyArray()
}
