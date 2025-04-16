package org.jetbrains.bazel.languages.bazelquery.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryElementTypes
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenSets
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenTypes

open class ParsingFlags(private val root: IElementType, val builder: PsiBuilder) : PsiBuilder by builder {
  private val utils = ParsingUtils(builder)

  fun parseFile(): ASTNode {
    val flagsList = builder.mark()
    while (!eof()) {
      when {
        utils.atAnyToken(BazelqueryTokenSets.FLAGS) -> parseFlag()
        else -> utils.advanceError("unexpected token: query option expected")
      }
    }
    flagsList.done(root)
    return treeBuilt
  }

  private fun parseFlag() {
    val flag = mark()

    if (utils.atToken(BazelqueryTokenTypes.UNFINISHED_FLAG)) {
      utils.advanceError("unfinished flag")
      utils.expectToken(BazelqueryTokenTypes.WHITE_SPACE)
    } else if (utils.atToken(BazelqueryTokenTypes.FLAG)) {
      advanceLexer()

      if (!utils.matchToken(BazelqueryTokenTypes.EQUALS)) {
        utils.advanceError("expected flag value")
      } else {
        if (utils.matchToken(BazelqueryTokenTypes.UNFINISHED_VAL)) {
          error("<quote> expected")
        } else if (!utils.matchAnyToken(BazelqueryTokenSets.FLAG_VALS)) {
          error("expected flag value")
        } else if (!eof()) {
          utils.expectToken(BazelqueryTokenTypes.WHITE_SPACE)
        }
      }
    } else {
      advanceLexer()
      while (utils.matchToken(BazelqueryTokenTypes.MISSING_SPACE)) {
        utils.advanceError("<space> expected")
      }
      utils.matchToken(BazelqueryTokenTypes.WHITE_SPACE)
    }

    flag.done(BazelqueryElementTypes.FLAG)
  }
}
