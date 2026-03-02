package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor

@ApiStatus.Internal
class StarlarkParenthesizedExpression(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitParenthesizedExpression(this)

  fun getTuple(): StarlarkTupleExpression? = findChildByClass(StarlarkTupleExpression::class.java)
}
