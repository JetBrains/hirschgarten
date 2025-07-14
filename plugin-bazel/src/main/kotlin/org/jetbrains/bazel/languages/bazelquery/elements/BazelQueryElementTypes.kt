package org.jetbrains.bazel.languages.bazelquery.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryCommand
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryFlag
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryFlagVal
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryInteger
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryWord

object BazelQueryElementTypes {
  val COMMAND = BazelQueryElementType("COMMAND")
  val WORD = BazelQueryElementType("WORD")
  val INTEGER = BazelQueryElementType("INTEGER")
  val FLAG = BazelQueryFlagsElementType("FLAG")
  val FLAG_VAL = BazelQueryFlagsElementType("FLAG_VAL")

  fun createElement(node: ASTNode): PsiElement =
    when (val type = node.elementType) {
      COMMAND -> BazelQueryCommand(node)
      WORD -> BazelQueryWord(node)
      FLAG -> BazelQueryFlag(node)
      FLAG_VAL -> BazelQueryFlagVal(node)
      INTEGER -> BazelQueryInteger(node)

      else -> error("Unknown element type: $type")
    }
}
