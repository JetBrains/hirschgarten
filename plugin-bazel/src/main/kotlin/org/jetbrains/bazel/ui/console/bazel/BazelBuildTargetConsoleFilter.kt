package org.jetbrains.bazel.ui.console

import com.intellij.execution.filters.ConsoleFilterProvider
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.HyperlinkInfo
import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.findFile
import com.intellij.openapi.vfs.findFileOrDirectory
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.descendantsOfType
import org.jetbrains.bazel.config.isBspProject
import org.jetbrains.bazel.config.rootDir
import org.jetbrains.bazel.label.Apparent
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.label.Main
import org.jetbrains.bazel.label.ResolvedLabel
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.references.BUILD_FILE_NAMES
import org.jetbrains.bazel.languages.starlark.repomapping.apparentRepoNameToCanonicalName
import org.jetbrains.bazel.languages.starlark.repomapping.canonicalRepoNameToPath

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

    val projectRoot =
      if (label.repo is Main) {
        // when the repo name is "", it is the root repo itself
        project.rootDir
      } else {
        // this is an external repo
        // now we check if it is an apparent name or canonical repository name
        val canonicalName =
          if (label.repo is Apparent) {
            project.apparentRepoNameToCanonicalName[label.repoName] ?: return null
          } else {
            label.repoName
          }
        val path = project.canonicalRepoNameToPath[canonicalName] ?: return null
        val virtualFileManager = VirtualFileManager.getInstance()
        virtualFileManager.findFileByNioPath(path) ?: return null
      }

    val hyperLinkInfo = getHyperLinkInfo(project, projectRoot, label.packagePath.toString(), label.targetName) ?: return null
    val highlightStartOffset = entireLength - line.length + highlightGroup.range.first
    val highlightEndOffset = entireLength - line.length + highlightGroup.range.last + 1

    return Filter.Result(highlightStartOffset, highlightEndOffset, hyperLinkInfo)
  }

  private fun getHyperLinkInfo(
    project: Project,
    virtualFileRoot: VirtualFile,
    buildFileName: String,
    target: String,
  ): HyperlinkInfo? {
    val virtualFile = buildFileName.toBazelFileInProject(virtualFileRoot) ?: return null

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

  private fun String.toBazelFileInProject(root: VirtualFile): VirtualFile? {
    val vf = root.findFileOrDirectory(this)
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
