package org.jetbrains.bazel.languages.starlark.psi.expressions.arguments

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement

interface StarlarkArgumentElement : StarlarkElement {
  fun getValue(): PsiElement? {
    var childNode: ASTNode? = node.lastChildNode
    while (childNode != null) {
      val type = childNode.elementType
      if (StarlarkElementTypes.EXPRESSIONS.contains(type)) {
        return childNode.psi
      }
      if (type == StarlarkTokenTypes.EQ) break
      childNode = childNode.treePrev
    }
    return null
  }
}
