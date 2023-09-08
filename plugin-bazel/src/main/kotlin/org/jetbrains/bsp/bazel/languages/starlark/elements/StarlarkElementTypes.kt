package org.jetbrains.bsp.bazel.languages.starlark.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType

object StarlarkElementTypes {
  // PsiElements will be declared here like:
  //    val STATEMENT =

  fun createElement(node: ASTNode): PsiElement {
    val type: IElementType = node.elementType
    // PsiElement will be created here like:
    // if (type == STATEMENT) {
    // return StarlarkStatementImpl(node)
    // }
    throw AssertionError("Unknown element type: $type")
  }
}
