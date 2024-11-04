package org.jetbrains.bazel.languages.starlark

import com.intellij.lang.ASTNode
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets

object StarlarkUtils {
  /**
   * original version: `com.jetbrains.python.psi.impl.PyPsiUtils.getNextNonWhitespaceSibling(com.intellij.lang.ASTNode)`
   */
  fun getNextNonWhitespaceSibling(after: ASTNode): ASTNode? = skipSiblingsForward(after, StarlarkTokenSets.WHITESPACE)

  fun skipSiblingsForward(node: ASTNode?, types: TokenSet): ASTNode? {
    var next = node?.treeNext
    while (next != null) {
      if (!types.contains(next.elementType)) {
        return next
      }
      next = next.treeNext
    }
    return null
  }
}
