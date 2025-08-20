package org.jetbrains.bazel.languages.starlark.references

import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.components.service
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.startOffset
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions
import org.jetbrains.bazel.languages.starlark.documentation.BazelGlobalFunctionArgumentDocumentationSymbol
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression

@Suppress("UnstableApiUsage")
class BazelGlobalFunctionArgumentReference(val element: StarlarkNamedArgumentExpression) : PsiSymbolReference {
  val textRange = element.getNameNode()?.textRange?.shiftLeft(element.startOffset)!!

  override fun getElement(): PsiElement = element

  override fun getRangeInElement(): TextRange = textRange

  override fun resolveReference(): Collection<Symbol?> {
    val argumentList = element.parent ?: return emptyList()
    val callExpression = argumentList.parent as? StarlarkCallExpression ?: return emptyList()
    val argumentName = element.getName() ?: return emptyList()
    val functionName = callExpression.getName() ?: return emptyList()
    val function = BazelGlobalFunctions.getFunctionByName(functionName) ?: return emptyList()
    val arg = function.params.find { it.name == argumentName } ?: return emptyList()
    return listOf(BazelGlobalFunctionArgumentDocumentationSymbol(arg, element.project))
  }
}
