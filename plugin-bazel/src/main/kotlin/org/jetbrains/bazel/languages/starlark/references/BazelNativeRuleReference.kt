package org.jetbrains.bazel.languages.starlark.references

import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.bazel.BazelNativeRule
import org.jetbrains.bazel.languages.starlark.documentation.BazelNativeRuleDocumentationSymbol
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.kotlin.idea.base.psi.relativeTo

@Suppress("UnstableApiUsage")
class BazelNativeRuleReference(private val element: StarlarkCallExpression, private val rule: BazelNativeRule) : PsiSymbolReference {
  val textRange = element.getNameNode()?.textRange?.relativeTo(element)!!

  override fun getElement(): PsiElement = element

  override fun getRangeInElement(): TextRange = textRange

  override fun resolveReference(): Collection<Symbol?> = listOf(BazelNativeRuleDocumentationSymbol(rule, element.project))
}
