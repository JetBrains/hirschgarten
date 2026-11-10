package org.jetbrains.bazel.languages.starlark.psi.statements

import com.intellij.lang.ASTNode
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenType
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.boundNames
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.isLoopTarget

@ApiStatus.Internal
class StarlarkForStatement(node: ASTNode) :
  StarlarkBaseElement(node),
  StarlarkStatementContainer,
  StarlarkAssigningStatement {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitForStatement(this)

  override fun operator(): StarlarkTokenType = StarlarkTokenTypes.IN_KEYWORD

  fun getLoopVariables(): List<StarlarkReferenceExpression> =
    children.firstOrNull { it.isLoopTarget() }?.boundNames().orEmpty()

  override fun getStatementLists(): List<StarlarkStatementList> = findChildrenByClass(StarlarkStatementList::class.java).toList()
}
