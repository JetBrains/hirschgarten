package org.jetbrains.bazel.languages.bazelquery.references

import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelquery.functions.BazelQueryFunction
import org.jetbrains.bazel.languages.bazelquery.functions.BazelQueryFunctionSymbol
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryCommand

@Suppress("UnstableApiUsage")
class BazelQueryFunctionReference(val element: BazelQueryCommand) : PsiSymbolReference {
  val textRange = element.name?.textRangeInParent ?: element.textRange!!

  override fun getElement(): PsiElement = element

  override fun getRangeInElement(): TextRange = textRange

  override fun resolveReference(): Collection<Symbol> =
    BazelQueryFunction
      .byName(rangeInElement.substring(element.text))
      ?.let { listOf(BazelQueryFunctionSymbol(it, element.project)) } ?: listOf()
}
