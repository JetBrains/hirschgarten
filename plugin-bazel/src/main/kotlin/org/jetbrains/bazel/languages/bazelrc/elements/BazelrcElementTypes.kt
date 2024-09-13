package org.jetbrains.bazel.languages.bazelrc.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcLine

object BazelrcElementTypes {
  val LINE = BazelrcElementType("LINE")

  fun createElement(node: ASTNode): PsiElement =
    when (node.elementType) {
      LINE -> BazelrcLine(node)

      else -> BazelrcLine(node)
    }
}
