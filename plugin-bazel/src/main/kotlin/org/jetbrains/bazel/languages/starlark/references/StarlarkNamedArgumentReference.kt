package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkKeywordVariadicParameter

class StarlarkNamedArgumentReference(element: StarlarkNamedArgumentExpression, range: TextRange) :
  StarlarkArgumentReference(element, range) {
  override fun resolve(): PsiElement? {
    val argumentName = element.name ?: return null
    val function = resolveFunction() ?: return null
    val parameters = function.getParameters()
    return parameters.find { it.name == argumentName } ?: parameters.find { it is StarlarkKeywordVariadicParameter }
  }
}
