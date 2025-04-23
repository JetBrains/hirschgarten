package org.jetbrains.bazel.languages.starlark.references

import com.intellij.model.Symbol
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.startOffset
import org.jetbrains.bazel.languages.starlark.bazel.BazelNativeRules
import org.jetbrains.bazel.languages.starlark.documentation.BazelNativeRuleArgumentDocumentationSymbol
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression

@Suppress("UnstableApiUsage")
class BazelNativeRuleArgumentReference(val element: StarlarkNamedArgumentExpression) : PsiSymbolReference {
  val textRange = element.getNameNode()?.textRange?.shiftLeft(element.startOffset)!!

  override fun getElement(): PsiElement = element

  override fun getRangeInElement(): TextRange = textRange

  override fun resolveReference(): Collection<Symbol?> {
    val argumentList = element.parent ?: return emptyList()
    val callExpression = argumentList.parent as? StarlarkCallExpression ?: return emptyList()
    val argumentName = element.getName() ?: return emptyList()
    val functionName = callExpression.getName() ?: return emptyList()
    val rule = BazelNativeRules.getRuleByName(functionName) ?: return emptyList()
    val arg = rule.arguments.find { it.name == argumentName } ?: return emptyList()
    return listOf(BazelNativeRuleArgumentDocumentationSymbol(arg, element.project))
  }
}
