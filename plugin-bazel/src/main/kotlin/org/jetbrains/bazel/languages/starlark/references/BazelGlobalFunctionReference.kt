package org.jetbrains.bazel.languages.starlark.references

import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunction
import org.jetbrains.bazel.languages.starlark.documentation.BazelGlobalFunctionDocumentationSymbol
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement.Companion.relativeTo
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression

@Suppress("UnstableApiUsage")
class BazelGlobalFunctionReference(private val element: StarlarkCallExpression, private val rule: BazelGlobalFunction) :
  PsiSymbolReference {
  val textRange = element.getNameNode()?.textRange?.relativeTo(element)!!

  override fun getElement(): PsiElement = element

  override fun getRangeInElement(): TextRange = textRange

  override fun resolveReference(): Collection<Symbol?> = listOf(BazelGlobalFunctionDocumentationSymbol(rule, element.project))
}
