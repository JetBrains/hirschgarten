package org.jetbrains.bazel.languages.projectview.references

import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelrc.flags.BazelFlagSymbol
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.projectview.psi.sections.ProjectViewPsiSectionItem

@Suppress("UnstableApiUsage")
class ProjectViewFlagReference(val element: ProjectViewPsiSectionItem) : PsiSymbolReference {
  override fun getElement(): PsiElement = element

  override fun getRangeInElement(): TextRange = element.firstChild.textRangeInParent

  override fun resolveReference(): Collection<Symbol> {
    val flagName = element.text
    val flag = Flag.byName(flagName) ?: return listOf()
    return listOf(BazelFlagSymbol(flag, element.project))
  }
}
