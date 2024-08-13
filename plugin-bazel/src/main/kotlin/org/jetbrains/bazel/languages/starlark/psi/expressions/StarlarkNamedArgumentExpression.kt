package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor

class StarlarkNamedArgumentExpression(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitNamedArgumentExpression(this)

  fun containsArgumentWithName(name: String): Boolean = node.findChildByType(StarlarkTokenTypes.IDENTIFIER)?.text == name

  fun isNameArgument(): Boolean = containsArgumentWithName("name")

  fun getArgumentStringValue(): String? = findChildByClass(StarlarkStringLiteralExpression::class.java)?.getStringContents()
}
