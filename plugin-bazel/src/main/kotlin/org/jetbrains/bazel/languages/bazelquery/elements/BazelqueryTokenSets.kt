package org.jetbrains.bazel.languages.bazelquery.elements

import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenTypes.COMMENT
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenTypes.DOUBLE_QUOTE
import org.jetbrains.bazel.languages.bazelquery.elements.BazelqueryTokenTypes.SINGLE_QUOTE

object BazelqueryTokenSets {

  val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)

  val COMMENTS = TokenSet.create(COMMENT)

  val QUOTES = TokenSet.create(DOUBLE_QUOTE, SINGLE_QUOTE)

  val WORDS = TokenSet.create(
    BazelqueryTokenTypes.UNQUOTED_WORD,
    BazelqueryTokenTypes.SQ_WORD,
    BazelqueryTokenTypes.DQ_WORD
  )

  val OPERATIONS = TokenSet.create(
    BazelqueryTokenTypes.UNION,
    BazelqueryTokenTypes.EXCEPT,
    BazelqueryTokenTypes.INTERSECT,
    BazelqueryTokenTypes.LET,
    BazelqueryTokenTypes.SET,
    BazelqueryTokenTypes.IN,
    BazelqueryTokenTypes.EQUALS,

  )
}
