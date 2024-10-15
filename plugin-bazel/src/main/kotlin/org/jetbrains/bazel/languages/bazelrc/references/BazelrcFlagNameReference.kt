package org.jetbrains.bazel.languages.bazelrc.references

import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelrc.flags.BazelFlagSymbol
import org.jetbrains.bazel.languages.bazelrc.flags.Flag
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFlag

@Suppress("UnstableApiUsage")
class BazelrcFlagNameReference(val element: BazelrcFlag) : PsiSymbolReference {
  val textRange = element.name?.textRangeInParent ?: element.textRange!!

  override fun getElement(): PsiElement = element

  override fun getRangeInElement(): TextRange = textRange

  override fun resolveReference(): Collection<Symbol> =
    Flag
      .byName(rangeInElement.substring(element.text))
      ?.let { listOf(BazelFlagSymbol(it, element.project)) } ?: listOf()
}
