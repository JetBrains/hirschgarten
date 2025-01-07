package org.jetbrains.bazel.languages.bazelquery.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement

import org.jetbrains.bazel.languages.bazelquery.psi.BazelqueryWord
import org.jetbrains.bazel.languages.bazelquery.psi.BazelqueryCommand
import org.jetbrains.bazel.languages.bazelquery.psi.BazelqueryPrompt
import org.jetbrains.bazel.languages.bazelquery.psi.BazelqueryQueryVal
import org.jetbrains.bazel.languages.bazelquery.psi.BazelqueryFlag


object BazelqueryElementTypes {
  val PROMPT = BazelqueryElementType("PROMPT")
  val COMMAND = BazelqueryElementType("COMMAND")
  val WORD = BazelqueryElementType("WORD")
  val INTEGER = BazelqueryElementType("INTEGER")
  val QUERY_VAL = BazelqueryElementType("QUERY_VAL")
  val FLAG = BazelqueryElementType("FLAG")
  val FLAG_VAL = BazelqueryElementType("FLAG_VAL")

  fun createElement(node: ASTNode): PsiElement =
    when (val type = node.elementType) {
      PROMPT -> BazelqueryPrompt(node)
      COMMAND -> BazelqueryCommand(node)
      WORD -> BazelqueryWord(node)
      QUERY_VAL -> BazelqueryQueryVal(node)
      FLAG -> BazelqueryFlag(node)
      FLAG_VAL -> BazelqueryFlag(node)
      INTEGER -> BazelqueryWord(node)

      else -> error("Unknown element type: $type")
    }
}
