package org.jetbrains.bazel.languages.projectview.annotation.quickfix

import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.Presentation
import com.intellij.modcommand.PsiUpdateModCommandAction
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.bazel.languages.projectview.BazelProjectViewBundle
import org.jetbrains.bazel.languages.projectview.ProjectViewSectionKey
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewElementFactory
import org.jetbrains.bazel.languages.projectview.psi.ProjectViewPsiFile
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem

/**
 * Implementation of [org.jetbrains.bazel.languages.projectview.checker.ProjectViewProblem.QuickFix.MergeIntoSection] quick fix.
 */
internal class MergeIntoSectionQuickFix(
  section: ProjectViewPsiSection,
  targetSectionKey: ProjectViewSectionKey<*>,
) : PsiUpdateModCommandAction<ProjectViewPsiSection>(section), DumbAware {

  private val targetSectionName = targetSectionKey.name

  override fun invoke(
    context: ActionContext,
    element: ProjectViewPsiSection,
    updater: ModPsiUpdater,
  ) {
    val projectViewFile = element.containingFile as? ProjectViewPsiFile ?: return
    val factory = ProjectViewElementFactory(context.project)
    val target = projectViewFile.getSection(targetSectionName)
    if (target == null) {
      element.getKeyword().replace(factory.createSection(targetSectionName).getKeyword())
      return
    }
    val content = element.contentLines()
    if (content.isNotEmpty()) {
      val source = factory.createSection(targetSectionName, content.map { it.text.trim() })
      val first = source.getColon()?.nextSibling ?: return
      val anchor = target.contentLines().lastOrNull() ?: target.getColon() ?: target.getKeyword()
      target.addRangeAfter(first, source.contentLines().last(), anchor)
    }
    element.deleteContent(content)
  }

  private fun ProjectViewPsiSection.contentLines(): List<PsiElement> {
    val lines = generateSequence(firstChild) { it.nextSibling }
      .filter { (it is ProjectViewPsiSectionItem && it.text.isNotBlank()) || it.elementType == ProjectViewTokenType.COMMENT }
      .toList()
    return lines.subList(0, lines.indexOfLast { it is ProjectViewPsiSectionItem || it.prevSibling.elementType == ProjectViewTokenType.WHITESPACE } + 1)
  }

  private fun ProjectViewPsiSection.deleteContent(content: List<PsiElement>) {
    val last = content.lastOrNull() ?: getColon() ?: getKeyword()
    val end = last.nextSibling?.takeIf { it.elementType == ProjectViewTokenType.NEWLINE } ?: last
    if (end.nextSibling == null) delete() else deleteChildRange(firstChild, end)
  }

  override fun getPresentation(
    context: ActionContext,
    element: ProjectViewPsiSection,
  ): Presentation = Presentation.of(BazelProjectViewBundle.message("quickfix.deprecated.section.replace.presentation", targetSectionName))

  override fun getFamilyName(): @IntentionFamilyName String =
    BazelProjectViewBundle.message("quickfix.deprecated.section.replace.description")
}
