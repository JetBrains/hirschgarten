package org.jetbrains.bazel.languages.starlark.elements

import com.intellij.psi.tree.TokenSet

object StarlarkTokenSets {
  val WHITESPACE = TokenSet.create(
    StarlarkTokenTypes.SPACE,
    StarlarkTokenTypes.TAB,
    StarlarkTokenTypes.LINE_BREAK,
    StarlarkTokenTypes.LINE_CONTINUATION
  )

  val COMMENT = TokenSet.create(StarlarkTokenTypes.COMMENT)

  val STRINGS = TokenSet.create(StarlarkTokenTypes.STRING, StarlarkTokenTypes.BYTES)

  val OPEN_BRACKETS = TokenSet.create(StarlarkTokenTypes.LBRACKET, StarlarkTokenTypes.LBRACE, StarlarkTokenTypes.LPAR)

  val CLOSE_BRACKETS = TokenSet.create(StarlarkTokenTypes.RBRACKET, StarlarkTokenTypes.RBRACE, StarlarkTokenTypes.RPAR)

  val STRING_NODES = TokenSet.create(StarlarkTokenTypes.STRING, StarlarkTokenTypes.BYTES)

  val RESERVED_KEYWORDS = TokenSet.create(
    StarlarkTokenTypes.AS_KEYWORD,
    StarlarkTokenTypes.ASSERT_KEYWORD,
    StarlarkTokenTypes.ASYNC_KEYWORD,
    StarlarkTokenTypes.AWAIT_KEYWORD,
    StarlarkTokenTypes.CLASS_KEYWORD,
    StarlarkTokenTypes.DEL_KEYWORD,
    StarlarkTokenTypes.EXCEPT_KEYWORD,
    StarlarkTokenTypes.FINALLY_KEYWORD,
    StarlarkTokenTypes.FROM_KEYWORD,
    StarlarkTokenTypes.GLOBAL_KEYWORD,
    StarlarkTokenTypes.IMPORT_KEYWORD,
    StarlarkTokenTypes.IS_KEYWORD,
    StarlarkTokenTypes.NONLOCAL_KEYWORD,
    StarlarkTokenTypes.RAISE_KEYWORD,
    StarlarkTokenTypes.TRY_KEYWORD,
    StarlarkTokenTypes.WHILE_KEYWORD,
    StarlarkTokenTypes.WITH_KEYWORD,
    StarlarkTokenTypes.YIELD_KEYWORD,
  )

  val PRIMARY_EXPRESSION_STARTERS = TokenSet.create(
    StarlarkTokenTypes.IDENTIFIER,
    StarlarkTokenTypes.INT,
    StarlarkTokenTypes.FLOAT,
    StarlarkTokenTypes.LPAR,
    StarlarkTokenTypes.LBRACKET,
    StarlarkTokenTypes.LBRACE,
    StarlarkTokenTypes.STRING,
    StarlarkTokenTypes.BYTES,
  )

  val ENDS_OF_STATEMENT = TokenSet.create(StarlarkTokenTypes.STATEMENT_BREAK, StarlarkTokenTypes.SEMICOLON)

  val COMPOUND_ASSIGN_OPERATIONS = TokenSet.create(
    StarlarkTokenTypes.PLUSEQ,
    StarlarkTokenTypes.MINUSEQ,
    StarlarkTokenTypes.MULTEQ,
    StarlarkTokenTypes.DIVEQ,
    StarlarkTokenTypes.FLOORDIVEQ,
    StarlarkTokenTypes.PERCEQ,
    StarlarkTokenTypes.ANDEQ,
    StarlarkTokenTypes.OREQ,
    StarlarkTokenTypes.XOREQ,
    StarlarkTokenTypes.LTLTEQ,
    StarlarkTokenTypes.GTGTEQ,
  )

  val COMPARISON_OPERATIONS = TokenSet.create(
    StarlarkTokenTypes.LT,
    StarlarkTokenTypes.GT,
    StarlarkTokenTypes.EQEQ,
    StarlarkTokenTypes.LE,
    StarlarkTokenTypes.GE,
    StarlarkTokenTypes.NE,
    StarlarkTokenTypes.IN_KEYWORD,
    StarlarkTokenTypes.NOT_KEYWORD,
  )

  val SHIFT_OPERATIONS = TokenSet.create(StarlarkTokenTypes.LTLT, StarlarkTokenTypes.GTGT)

  val ADDITIVE_OPERATIONS = TokenSet.create(StarlarkTokenTypes.PLUS, StarlarkTokenTypes.MINUS)

  val MULTIPLICATIVE_OPERATIONS = TokenSet.create(
    StarlarkTokenTypes.MULT, StarlarkTokenTypes.FLOORDIV, StarlarkTokenTypes.DIV, StarlarkTokenTypes.PERC,
  )

  val UNARY_OPERATIONS = TokenSet.create(
    StarlarkTokenTypes.PLUS, StarlarkTokenTypes.MINUS, StarlarkTokenTypes.TILDE, StarlarkTokenTypes.NOT_KEYWORD,
  )
}
