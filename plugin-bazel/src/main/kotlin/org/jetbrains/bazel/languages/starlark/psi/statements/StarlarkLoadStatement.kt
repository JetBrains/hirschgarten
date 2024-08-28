package org.jetbrains.bazel.languages.starlark.psi.statements

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression

class StarlarkLoadStatement(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitLoadStatement(this)

  fun getLoadedFileNamePsi(): StarlarkStringLiteralExpression? = findChildByType(StarlarkElementTypes.STRING_LITERAL_EXPRESSION)

  fun getLoadedSymbolsPsi(): List<StarlarkElement> = children.filterIsInstance<StarlarkLoadValue>().toList()
}
