package org.jetbrains.bazel.languages.starlark

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiWhiteSpace

object StarlarkUtils {
  /**
   * original version: `com.jetbrains.python.psi.impl.PyPsiUtils.getNextNonWhitespaceSibling(com.intellij.lang.ASTNode)`
   */
  fun getNextNonWhitespaceSibling(after: ASTNode): ASTNode? {
    var next = after.treeNext
    while (next != null) {
      if (next !is PsiWhiteSpace) {
        return next
      }
      next = next.treeNext
    }
    return null
  }
}
