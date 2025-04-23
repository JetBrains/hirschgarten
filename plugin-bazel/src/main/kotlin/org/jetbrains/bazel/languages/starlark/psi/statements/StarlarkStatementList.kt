package org.jetbrains.bazel.languages.starlark.psi.statements

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkFunctionDeclaration

class StarlarkStatementList(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitStatementListImpl(this)

  fun getAssignments(): List<StarlarkAssignmentStatement> = findChildrenByClass(StarlarkAssignmentStatement::class.java).toList()

  fun getFunctionDeclarations(): List<StarlarkFunctionDeclaration> = findChildrenByClass(StarlarkFunctionDeclaration::class.java).toList()
}
