package org.jetbrains.bazel.languages.starlark.psi.expressions

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElementVisitor

@ApiStatus.Internal
class StarlarkDictLiteralExpression(node: ASTNode) : StarlarkBaseElement(node) {
  override fun acceptVisitor(visitor: StarlarkElementVisitor) = visitor.visitDictLiteralExpression(this)

  fun getKeys(): List<PsiElement>? {
    val keys = mutableListOf<PsiElement>()
    var current = firstChild
    while (current != null) {
      when (current) {
        is StarlarkKeyValueExpression -> keys.add(current.node.findChildByType(StarlarkElementTypes.EXPRESSIONS)?.psi ?: return null)
        is StarlarkBaseElement -> return null
      }
      current = current.nextSibling
    }
    return keys
  }
}
