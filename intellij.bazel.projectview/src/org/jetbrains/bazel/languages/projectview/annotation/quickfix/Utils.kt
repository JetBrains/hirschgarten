package org.jetbrains.bazel.languages.projectview.annotation.quickfix

import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSection
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem

internal fun ProjectViewPsiSection.contentLines(): List<PsiElement> {
  val lines = generateSequence(firstChild) { it.nextSibling }
    .filter { (it is ProjectViewPsiSectionItem && it.text.isNotBlank()) || it.elementType == ProjectViewTokenType.COMMENT }
    .toList()
  return lines.subList(0, lines.indexOfLast { it is ProjectViewPsiSectionItem || it.prevSibling.elementType == ProjectViewTokenType.WHITESPACE } + 1)
}

internal fun ProjectViewPsiSection.deleteContent(content: List<PsiElement> = contentLines()) {
  val last = content.lastOrNull() ?: getColon() ?: getKeyword()
  val end = last.nextSibling?.takeIf { it.elementType == ProjectViewTokenType.NEWLINE } ?: last
  if (end.nextSibling == null) delete() else deleteChildRange(firstChild, end)
}
