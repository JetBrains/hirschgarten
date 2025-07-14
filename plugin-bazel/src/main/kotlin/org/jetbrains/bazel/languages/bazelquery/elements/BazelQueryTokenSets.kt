package org.jetbrains.bazel.languages.bazelquery.elements

import com.intellij.psi.TokenType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.bazelquery.elements.BazelQueryTokenTypes.COMMENT

object BazelQueryTokenSets {
  val WHITE_SPACES = TokenSet.create(TokenType.WHITE_SPACE)

  val COMMENTS = TokenSet.create(COMMENT)

  val WORDS =
    TokenSet.create(
      BazelQueryTokenTypes.UNQUOTED_WORD,
      BazelQueryTokenTypes.SQ_WORD,
      BazelQueryTokenTypes.DQ_WORD,
      BazelQueryTokenTypes.SQ_UNFINISHED,
      BazelQueryTokenTypes.DQ_UNFINISHED,
      BazelQueryTokenTypes.SQ_EMPTY,
      BazelQueryTokenTypes.DQ_EMPTY,
      BazelQueryTokenTypes.ERR_WORD,
    )

  val PATTERNS =
    TokenSet.create(
      BazelQueryTokenTypes.UNQUOTED_WORD,
      BazelQueryTokenTypes.SQ_PATTERN,
      BazelQueryTokenTypes.DQ_PATTERN,
    )

  val FLAGS =
    TokenSet.create(
      BazelQueryTokenTypes.FLAG,
      BazelQueryTokenTypes.FLAG_NO_VAL,
      BazelQueryTokenTypes.UNFINISHED_FLAG,
    )

  val FLAG_VALS =
    TokenSet.create(
      BazelQueryTokenTypes.UNQUOTED_VAL,
      BazelQueryTokenTypes.SQ_VAL,
      BazelQueryTokenTypes.DQ_VAL,
      BazelQueryTokenTypes.UNFINISHED_VAL,
    )

  val OPERATIONS =
    TokenSet.create(
      BazelQueryTokenTypes.UNION,
      BazelQueryTokenTypes.EXCEPT,
      BazelQueryTokenTypes.INTERSECT,
      BazelQueryTokenTypes.LET,
      BazelQueryTokenTypes.SET,
      BazelQueryTokenTypes.IN,
      BazelQueryTokenTypes.EQUALS,
    )

  val COMMANDS =
    TokenSet.create(
      BazelQueryTokenTypes.ALLPATHS,
      BazelQueryTokenTypes.ATTR,
      BazelQueryTokenTypes.BUILDFILES,
      BazelQueryTokenTypes.RBUILDFILES,
      BazelQueryTokenTypes.DEPS,
      BazelQueryTokenTypes.FILTER,
      BazelQueryTokenTypes.KIND,
      BazelQueryTokenTypes.LABELS,
      BazelQueryTokenTypes.LOADFILES,
      BazelQueryTokenTypes.RDEPS,
      BazelQueryTokenTypes.ALLRDEPS,
      BazelQueryTokenTypes.SAME_PKG_DIRECT_RDEPS,
      BazelQueryTokenTypes.SIBLINGS,
      BazelQueryTokenTypes.SOME,
      BazelQueryTokenTypes.SOMEPATH,
      BazelQueryTokenTypes.TESTS,
      BazelQueryTokenTypes.VISIBLE,
    )
}
