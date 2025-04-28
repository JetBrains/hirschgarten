package org.jetbrains.bazel.ui.console

import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.languages.starlark.references.resolveLabel

class BazelBuildTargetConsoleFilter(private val project: Project) : Filter {
  private val highlightGroupName = "highlightGroup"

  // The set of characters allowed in labels is taken from https://bazel.build/concepts/labels#target-names
  private val bazelTargetRegex =
    """(^|\W)
        (?<$highlightGroupName>
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
    val highlightGroup = groups[highlightGroupName] ?: return null
    val label = Label.parseOrNull(highlightGroup.value) as? ResolvedLabel ?: return null
    val psi = resolveLabel(project, label) ?: return null
    val containingFile = psi.containingFile ?: return null
    val document = PsiDocumentManager.getInstance(project).getDocument(containingFile) ?: return null
    val textOffset = psi.textOffset
    val documentLine = document.getLineNumber(textOffset)
    val documentColumn = textOffset - document.getLineStartOffset(documentLine)
    val hyperLinkInfo = OpenFileHyperlinkInfo(project, containingFile.virtualFile ?: return null, documentLine, documentColumn)
    val highlightStartOffset = entireLength - line.length + highlightGroup.range.first
    val highlightEndOffset = entireLength - line.length + highlightGroup.range.last + 1
    return Filter.Result(highlightStartOffset, highlightEndOffset, hyperLinkInfo)
  }
}

class BazelBuildTargetConsoleFilterProvider : ConsoleFilterProvider {
  override fun getDefaultFilters(project: Project): Array<out Filter> =
    if (project.isBazelProject) arrayOf(BazelBuildTargetConsoleFilter(project)) else emptyArray()
}
