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
    BazelqueryTokenTypes.DQ_WORD,
    BazelqueryTokenTypes.SQ_UNFINISHED,
    BazelqueryTokenTypes.DQ_UNFINISHED,
    BazelqueryTokenTypes.SQ_EMPTY,
    BazelqueryTokenTypes.DQ_EMPTY,
  )

  val FLAGS = TokenSet.create(
    BazelqueryTokenTypes.FLAG,
    BazelqueryTokenTypes.FLAG_NO_VAL
  )

  val FLAG_VALS = TokenSet.create(
    BazelqueryTokenTypes.UNQUOTED_VAL,
    BazelqueryTokenTypes.SQ_VAL,
    BazelqueryTokenTypes.DQ_VAL,
    BazelqueryTokenTypes.UNFINISHED_VAL
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

  val COMMANDS = TokenSet.create(
    BazelqueryTokenTypes.ALLPATHS,
    BazelqueryTokenTypes.ATTR,
    BazelqueryTokenTypes.BUILDFILES,
    BazelqueryTokenTypes.RBUILDFILES,
    BazelqueryTokenTypes.DEPS,
    BazelqueryTokenTypes.FILTER,
    BazelqueryTokenTypes.KIND,
    BazelqueryTokenTypes.LABELS,
    BazelqueryTokenTypes.LOADFILES,
    BazelqueryTokenTypes.RDEPS,
    BazelqueryTokenTypes.ALLRDEPS,
    BazelqueryTokenTypes.SAME_PKG_DIRECT_RDEPS,
    BazelqueryTokenTypes.SIBLINGS,
    BazelqueryTokenTypes.SOME,
    BazelqueryTokenTypes.SOMEPATH,
    BazelqueryTokenTypes.TESTS,
    BazelqueryTokenTypes.VISIBLE
  )
}
