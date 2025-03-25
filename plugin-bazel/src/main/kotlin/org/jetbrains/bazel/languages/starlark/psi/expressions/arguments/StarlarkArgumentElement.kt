package org.jetbrains.bazel.languages.starlark.psi.expressions.arguments

import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkBaseElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkElement

interface StarlarkArgumentElement : StarlarkElement {
  fun getValue(): PsiElement? {
    var child_node = node.lastChildNode
    while (child_node != null) {
      var type = child_node.elementType
      if (StarlarkElementTypes.EXPRESSIONS.contains(type)) {
        return child_node.psi
      }
      if (type == StarlarkTokenTypes.EQ || type == StarlarkTokenTypes.MULT) {
        break;
      }
      child_node = child_node.treePrev
    }
    return null
  }
}
