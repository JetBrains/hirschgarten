package org.jetbrains.bazel.languages.bazelquery.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryElementTypes
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenTypes
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenSets
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenType

open class ParsingFlags(private val root: IElementType, val builder: PsiBuilder) : PsiBuilder by builder {

  // _____Matching and advancing_____

  private fun atToken(tokenType: IElementType?): Boolean = builder.tokenType === tokenType

  private fun atAnyToken(tokenTypes: TokenSet): Boolean = tokenTypes.contains(tokenType)

  private fun matchToken(tokenType: IElementType): Boolean {
    if (atToken(tokenType)) {
      advanceLexer()
      return true
    }
    return false
  }

  private fun expectToken(tokenType: IElementType): Boolean {
    if (atToken(tokenType)) {
      advanceLexer()
      return true
    }
    advanceError("<$tokenType> expected")
    return false
  }

  private fun matchAnyToken(tokens: TokenSet): Boolean {
    if (atAnyToken(tokens)) {
      advanceLexer()
      return true
    }
    return false
  }

  private fun advanceError(message: String) {
    val err = mark()
    advanceLexer()
    err.error(message)
  }


  // _____Parsing_____

  fun parseFile(): ASTNode {
    val prompt = builder.mark()

    while (!eof()) {
      when {
        atAnyToken(BazelqueryTokenSets.FLAGS) -> parseFlag()
        else -> advanceError("Invalid content")
      }
    }

    prompt.done(root)
    return treeBuilt
  }

  private fun parseFlag() {
    val flag = mark()

    if(atToken(BazelqueryTokenTypes.UNFINISHED_FLAG)) {
      if (!eof()) advanceError("Unfinished flag")
      else error("Unfinished flag1")
      expectToken(BazelqueryTokenTypes.WHITE_SPACE)
    }
    else if(atToken(BazelqueryTokenTypes.FLAG)) {
      advanceLexer()

      if(!matchToken(BazelqueryTokenTypes.EQUALS)) advanceError("Flag value expected1")
      else {
        if(matchToken(BazelqueryTokenTypes.UNFINISHED_VAL)) error("Quote expected")
        else if(!matchAnyToken(BazelqueryTokenSets.FLAG_VALS)) error("Flag value expected2")
        else if(!eof()) expectToken(BazelqueryTokenTypes.WHITE_SPACE)
      }
    }
    else {
      advanceLexer()
      while(matchToken(BazelqueryTokenTypes.MISSING_SPACE)) {
        advanceError("Whitespace expected")
      }
      matchToken(BazelqueryTokenTypes.WHITE_SPACE)
    }

    flag.done(BazelqueryElementTypes.FLAG)
  }
}

