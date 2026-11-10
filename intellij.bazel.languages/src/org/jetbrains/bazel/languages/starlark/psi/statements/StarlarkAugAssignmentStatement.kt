package org.jetbrains.bazel.languages.starlark.psi.statements

import com.intellij.lang.ASTNode
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor

@ApiStatus.Internal
class StarlarkAugAssignmentStatement(node: ASTNode) : StarlarkBaseElement(node), StarlarkAssigningStatement {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitAugAssignmentStatement(this)

  override fun operator(): IElementType = node
    .getChildren(StarlarkTokenSets.COMPOUND_ASSIGN_OPERATIONS)
    .first()
    .elementType
}
