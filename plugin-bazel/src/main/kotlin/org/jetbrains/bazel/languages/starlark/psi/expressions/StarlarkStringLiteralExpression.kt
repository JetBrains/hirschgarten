package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor

class StarlarkStringLiteralExpression(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitStringLiteralExpression(this)

  fun getStringContents() =
    node.findChildByType(StarlarkTokenTypes.STRING)?.text
      ?.let {
        if (it.startsWith("\"\"\""))
          it.removeSurrounding("\"\"\"")
        else 
          it.removeSurrounding("\"")
      }
}
