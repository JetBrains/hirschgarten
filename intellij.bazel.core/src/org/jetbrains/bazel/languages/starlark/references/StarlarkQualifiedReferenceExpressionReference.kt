package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkAssignmentStatement
import org.jetbrains.kotlin.psi.psiUtil.getChildOfType

internal class StarlarkQualifiedReferenceExpressionReference (element: StarlarkReferenceExpression) :
  PsiReferenceBase<StarlarkReferenceExpression>(element, TextRange(0, element.textLength), false) {

  override fun resolve(): PsiElement? {
    val identifier = element.name ?: return null
    val qualifiedPartResolved = element.getQualifierExpression()?.reference?.resolve() ?: return null
    val assignment = qualifiedPartResolved.parent as? StarlarkAssignmentStatement ?: return null
    val assignedValue = assignment.getChildOfType<StarlarkCallExpression>() ?: return null
    if (assignedValue.getNameNode()?.text != "struct") return null
    val definition = assignedValue.getArgumentList()?.getKeywordArgument(identifier) ?: return null

    return (definition.lastChild as? StarlarkReferenceExpression)?.reference?.resolve() ?: definition
  }

}
