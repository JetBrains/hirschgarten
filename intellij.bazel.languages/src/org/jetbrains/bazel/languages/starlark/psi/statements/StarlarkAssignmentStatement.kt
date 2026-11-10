package org.jetbrains.bazel.languages.starlark.psi.statements

import com.intellij.lang.ASTNode
import com.intellij.util.Processor
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenType
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.boundNames
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.isBinding

@ApiStatus.Internal
class StarlarkAssignmentStatement(node: ASTNode) : StarlarkBaseElement(node), StarlarkAssigningStatement {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitAssignmentStatement(this)

  override fun getName(): String? = getBoundName()?.name ?: super.getName()

  override fun operator(): StarlarkTokenType = StarlarkTokenTypes.EQ

  fun check(processor: Processor<StarlarkElement>): Boolean =
    leftHandSide()?.boundNames().orEmpty().all { processor.process(it) }

  fun isTopLevel(): Boolean = parent is StarlarkFile

  private fun getBoundName(): StarlarkReferenceExpression? = (leftHandSide() as? StarlarkReferenceExpression)?.takeIf { it.isBinding() }
}
