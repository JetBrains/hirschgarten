package org.jetbrains.bazel.languages.starlark.psi.statements

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor

class StarlarkIfStatement(node: ASTNode) :
  StarlarkBaseElement(node),
  StarlarkStatementContainer {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitIfStatement(this)

  override fun getStatementLists(): List<StarlarkStatementList> = findChildrenByClass(StarlarkStatementList::class.java).toList()
}
