package org.jetbrains.bazel.languages.projectview.documentation

import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.jetbrains.bazel.languages.bazelrc.flags.BazelFlagSymbol
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.projectview.base.ProjectViewLanguage
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem

class ProjectViewFlagPsiDocumentationTargetProvider : PsiDocumentationTargetProvider {
  override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
    if (element.language !is ProjectViewLanguage) return null
    if (element !is ProjectViewPsiSectionItem) return null
    if (!(element.firstChild != null && element.firstChild == element.lastChild && element.firstChild.elementType == ProjectViewTokenType.IDENTIFIER)) {
      return null
    }
    val flag = Flag.byName(element.text) ?: return null
    val flagSymbol = BazelFlagSymbol(flag, element.project)
    return flagSymbol.documentationTarget
  }
}
