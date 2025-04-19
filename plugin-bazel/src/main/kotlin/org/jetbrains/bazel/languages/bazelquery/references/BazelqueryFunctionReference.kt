package org.jetbrains.bazel.languages.bazelquery.references

import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelquery.functions.BazelqueryFunction
import org.jetbrains.bazel.languages.bazelquery.functions.BazelqueryFunctionSymbol
import org.jetbrains.bazel.languages.bazelquery.psi.BazelqueryCommand

@Suppress("UnstableApiUsage")
class BazelqueryFunctionReference(val element: BazelqueryCommand) : PsiSymbolReference {
  val textRange = element.name?.textRangeInParent ?: element.textRange!!

  override fun getElement(): PsiElement = element

  override fun getRangeInElement(): TextRange = textRange

  override fun resolveReference(): Collection<Symbol> =
    BazelqueryFunction
      .byName(rangeInElement.substring(element.text))
      ?.let { listOf(BazelqueryFunctionSymbol(it, element.project)) } ?: listOf()
}
