package org.jetbrains.bazel.languages.bazelrc.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFlag
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcLine

object BazelrcElementTypes {
  val LINE = BazelrcElementType("LINE")
  val FLAG = BazelrcElementType("FLAG")

  fun createElement(node: ASTNode): PsiElement =
    when (val type = node.elementType) {
      LINE -> BazelrcLine(node)
      FLAG -> BazelrcFlag(node)

      else -> error("Unknown element type: $type")
    }
}
