package org.jetbrains.bazel.ui.console

import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.findFileOrDirectory
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.descendantsOfType
import org.jetbrains.bazel.config.isBspProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.references.BUILD_FILE_NAMES

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
    val hyperLinkInfo = getHyperLinkInfo(project, pathGroup.value, target.value) ?: return null
    val highlightStartOffset = entireLength - line.length + highlightGroup.range.first
    val highlightEndOffset = entireLength - line.length + highlightGroup.range.last + 1

    return Filter.Result(highlightStartOffset, highlightEndOffset, hyperLinkInfo)
  }

  private fun getHyperLinkInfo(
    project: Project,
    buildFileName: String,
    target: String,
  ): HyperlinkInfo? {
    val virtualFile = buildFileName.toBazelFileInProject() ?: return null

    return runReadAction {
      val psiElement =
        virtualFile
          .findPsiFile(project)
          ?.descendantsOfType<StarlarkNamedArgumentExpression>()
          ?.filter { it.isNameArgument() }
          ?.firstOrNull {
            it.getArgumentStringValue()?.let { name -> Label.parseOrNull(name)?.targetName } == target
          }
      OpenFileHyperlinkInfo(project, virtualFile, psiElement?.calculateLineNumber() ?: 0, 0)
    }
  }

  private fun String.toBazelFileInProject(): VirtualFile? {
    val vf = project.rootDir.findFileOrDirectory(this)
    if (vf == null || !vf.isDirectory) return null
    return BUILD_FILE_NAMES.mapNotNull { vf.findFile(it) }.firstOrNull()
  }

  private fun PsiElement.calculateLineNumber(): Int? =
    PsiDocumentManager.getInstance(project).getDocument(containingFile)?.getLineNumber(textOffset)
}

class BazelBuildTargetConsoleFilterProvider : ConsoleFilterProvider {
  override fun getDefaultFilters(project: Project): Array<out Filter> =
    if (project.isBspProject) arrayOf(BazelBuildTargetConsoleFilter(project)) else emptyArray()
}
