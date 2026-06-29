package org.jetbrains.bazel.languages.starlark.references

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

internal class StarlarkQualifiedReferenceExpressionReference (element: StarlarkReferenceExpression) :
  PsiReferenceBase<StarlarkReferenceExpression>(
    element,
    element.getNameIdentifier()?.textRangeInParent ?: TextRange(0, element.textLength),
    false,
  ) {

  override fun resolve(): PsiElement? {
    val identifier = element.name ?: return null
    val definition = element.getStructArgumentList()?.getKeywordArgument(identifier) ?: return null

    return (definition.lastChild as? StarlarkReferenceExpression)?.reference?.resolve() ?: definition
  }

  override fun getVariants(): Array<LookupElementBuilder> =
    element.getStructArgumentList()
      ?.getArguments()
      ?.mapNotNull { it.name }
      ?.map { LookupElementBuilder.create(it) }
      ?.toTypedArray()
    ?: emptyArray()
}

private fun StarlarkReferenceExpression.getStructArgumentList(): StarlarkArgumentList? {
  val qualifiedPartResolved = getQualifierExpression()?.reference?.resolve() ?: return null
  val structCall = when (qualifiedPartResolved) {
    is StarlarkNamedArgumentExpression -> qualifiedPartResolved.getValue() as? StarlarkCallExpression
    else -> (qualifiedPartResolved.parent as? StarlarkAssignmentStatement)?.getChildOfType()
  } ?: return null
  if (structCall.getCalledFunctionName() != "struct") return null
  return structCall.getArgumentList()
}
