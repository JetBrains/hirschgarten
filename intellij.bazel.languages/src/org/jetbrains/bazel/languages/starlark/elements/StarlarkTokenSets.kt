package org.jetbrains.bazel.languages.starlark.elements

import com.intellij.psi.tree.TokenSet
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes

@ApiStatus.Internal
object StarlarkTokenSets {
  val WHITESPACE =
    TokenSet.create(
      StarlarkTokenTypes.SPACE,
      StarlarkTokenTypes.TAB,
      StarlarkTokenTypes.LINE_BREAK,
      StarlarkTokenTypes.LINE_CONTINUATION,
    )

  val IDENTIFIER = TokenSet.create(StarlarkTokenTypes.IDENTIFIER)

  val COMMENT = TokenSet.create(StarlarkTokenTypes.COMMENT)

  val WHITESPACE_AND_COMMENTS = TokenSet.orSet(WHITESPACE, COMMENT)

  /**
   * The keywords that can only start a statement or a clause and cannot be inside brackets (`[` `(` or `{`) in valid code.
   * `if`, `else` and `for` are not here, because an expression can hold them, as in `[x for x in y if x]`.
   * This set partially overlaps with [COMPOUND_STATEMENT_KEYWORDS] on purpose, because the two sets have different uses.
   * Some of these keywords, `def` and `elif`, also start a compound statement when a `:` comes after them.
   */
  val KEYWORDS_OUTSIDE_BRACKETS =
    TokenSet.create(
      StarlarkTokenTypes.BREAK_KEYWORD,
      StarlarkTokenTypes.CONTINUE_KEYWORD,
      StarlarkTokenTypes.DEF_KEYWORD,
      StarlarkTokenTypes.ELIF_KEYWORD,
      StarlarkTokenTypes.LOAD_KEYWORD,
      StarlarkTokenTypes.PASS_KEYWORD,
      StarlarkTokenTypes.RETURN_KEYWORD,
    )

  /**
   * The keywords that can start a compound statement (or one of its clauses) when a `:` comes after them, as in `if x:`.
   * This set partially overlaps with [KEYWORDS_OUTSIDE_BRACKETS] on purpose, because the two sets have different uses.
   * Some of these keywords, `def` and `elif`, also cannot be inside brackets in valid code.
   */
  val COMPOUND_STATEMENT_KEYWORDS =
    TokenSet.create(
      StarlarkTokenTypes.DEF_KEYWORD,
      StarlarkTokenTypes.ELIF_KEYWORD,
      StarlarkTokenTypes.ELSE_KEYWORD,
      StarlarkTokenTypes.FOR_KEYWORD,
      StarlarkTokenTypes.IF_KEYWORD,
    )

  val SPACES_AND_TABS = TokenSet.create(StarlarkTokenTypes.SPACE, StarlarkTokenTypes.TAB)

  val STRINGS = TokenSet.create(StarlarkTokenTypes.STRING, StarlarkTokenTypes.BYTES)

  val OPEN_BRACKETS = TokenSet.create(StarlarkTokenTypes.LBRACKET, StarlarkTokenTypes.LBRACE, StarlarkTokenTypes.LPAR)

  val CLOSE_BRACKETS = TokenSet.create(StarlarkTokenTypes.RBRACKET, StarlarkTokenTypes.RBRACE, StarlarkTokenTypes.RPAR)

  val LBRACKET_OR_LPAR = TokenSet.create(StarlarkTokenTypes.LBRACKET, StarlarkTokenTypes.LPAR)

  val RESERVED_KEYWORDS =
    TokenSet.create(
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

  val PRIMARY_EXPRESSION_STARTERS =
    TokenSet.create(
      StarlarkTokenTypes.IDENTIFIER,
      StarlarkTokenTypes.INT,
      StarlarkTokenTypes.FLOAT,
      StarlarkTokenTypes.LPAR,
      StarlarkTokenTypes.LBRACKET,
      StarlarkTokenTypes.LBRACE,
      StarlarkTokenTypes.STRING,
      StarlarkTokenTypes.BYTES,
      StarlarkTokenTypes.NONE_KEYWORD,
      StarlarkTokenTypes.TRUE_KEYWORD,
      StarlarkTokenTypes.FALSE_KEYWORD,
    )

  /**
   * The tokens that start an operand: a name, a number, a string, `None`, `True` or `False`.
   * An open bracket is not here, because after an expression it starts a call or an index, as in `f(x)`.
   */
  val OPERAND_STARTS = TokenSet.andNot(PRIMARY_EXPRESSION_STARTERS, OPEN_BRACKETS)

  /**
   * The tokens that can end an expression: an operand or a closing bracket.
   * An operand right after one of them, on the next line, starts a new statement, as `y` in `x = f(a⏎y = 2`.
   */
  val EXPRESSION_ENDS = TokenSet.orSet(OPERAND_STARTS, CLOSE_BRACKETS)

  val COMPOUND_ASSIGN_OPERATIONS =
    TokenSet.create(
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

  val ASSIGNMENT_OPERATORS =
    TokenSet.create(
      StarlarkTokenTypes.EQ,
      *COMPOUND_ASSIGN_OPERATIONS.types,
      StarlarkTokenTypes.IN_KEYWORD,
    )

  val COMPARISON_OPERATIONS =
    TokenSet.create(
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

  val MULTIPLICATIVE_OPERATIONS =
    TokenSet.create(
      StarlarkTokenTypes.MULT,
      StarlarkTokenTypes.FLOORDIV,
      StarlarkTokenTypes.DIV,
      StarlarkTokenTypes.PERC,
    )

  val BINARY_OPERATIONS =
    TokenSet.orSet(
      TokenSet.create(
        StarlarkTokenTypes.AND_KEYWORD,
        StarlarkTokenTypes.AND,
        StarlarkTokenTypes.OR_KEYWORD,
        StarlarkTokenTypes.OR,
        StarlarkTokenTypes.XOR,
        StarlarkTokenTypes.EXP,
        StarlarkTokenTypes.NOT_KEYWORD,
        StarlarkTokenTypes.IN_KEYWORD,
        StarlarkTokenTypes.IS_KEYWORD,
      ),
      ADDITIVE_OPERATIONS,
      COMPARISON_OPERATIONS,
      MULTIPLICATIVE_OPERATIONS,
      SHIFT_OPERATIONS,
    )

  val EXPRESSIONS =
    TokenSet.create(
      StarlarkTokenTypes.ARGUMENT_EXPRESSION,
      StarlarkTokenTypes.ARGUMENT_LIST,
      StarlarkTokenTypes.BINARY_EXPRESSION,
      StarlarkTokenTypes.CALL_EXPRESSION,
      StarlarkTokenTypes.CONDITIONAL_EXPRESSION,
      StarlarkTokenTypes.DICT_COMP_EXPRESSION,
      StarlarkTokenTypes.DICT_LITERAL_EXPRESSION,
      StarlarkTokenTypes.DOUBLE_STAR_EXPRESSION,
      StarlarkTokenTypes.FALSE_LITERAL_EXPRESSION,
      StarlarkTokenTypes.FLOAT_LITERAL_EXPRESSION,
      StarlarkTokenTypes.GENERATOR_EXPRESSION,
      StarlarkTokenTypes.INTEGER_LITERAL_EXPRESSION,
      StarlarkTokenTypes.KEY_VALUE_EXPRESSION,
      StarlarkTokenTypes.LAMBDA_EXPRESSION,
      StarlarkTokenTypes.LIST_COMP_EXPRESSION,
      StarlarkTokenTypes.LIST_LITERAL_EXPRESSION,
      StarlarkTokenTypes.NAMED_ARGUMENT_EXPRESSION,
      StarlarkTokenTypes.NONE_LITERAL_EXPRESSION,
      StarlarkTokenTypes.PARENTHESIZED_EXPRESSION,
      StarlarkTokenTypes.PREFIX_EXPRESSION,
      StarlarkTokenTypes.REFERENCE_EXPRESSION,
      StarlarkTokenTypes.SLICE_EXPRESSION,
      StarlarkTokenTypes.STAR_ARGUMENT_EXPRESSION,
      StarlarkTokenTypes.STAR_EXPRESSION,
      StarlarkTokenTypes.STRING_LITERAL_EXPRESSION,
      StarlarkTokenTypes.SUBSCRIPTION_EXPRESSION,
      StarlarkTokenTypes.TRUE_LITERAL_EXPRESSION,
      StarlarkTokenTypes.TUPLE_EXPRESSION,
    )

  /** The tokens that come before the first token of a logical line: a statement break, an indent and a dedent. */
  val LINE_STARTS =
    TokenSet.create(
      StarlarkTokenTypes.STATEMENT_BREAK,
      StarlarkTokenTypes.INDENT,
      StarlarkTokenTypes.DEDENT,
    )

  val SPACES_TABS_AND_COMMENTS = TokenSet.orSet(SPACES_AND_TABS, COMMENT)

  /** The tokens between two simple statements: a statement break and `;`. */
  val STATEMENT_SEPARATORS = TokenSet.create(StarlarkTokenTypes.STATEMENT_BREAK, StarlarkTokenTypes.SEMICOLON)

  /** The tokens that cannot start a statement. */
  val NO_STATEMENT_STARTS =
    TokenSet.orSet(
      COMPOUND_ASSIGN_OPERATIONS,
      CLOSE_BRACKETS,
      TokenSet.create(
        StarlarkTokenTypes.EQ,
        StarlarkTokenTypes.COMMA,
        StarlarkTokenTypes.COLON,
        StarlarkTokenTypes.DOT,
        StarlarkTokenTypes.ELSE_KEYWORD,
        StarlarkTokenTypes.ELIF_KEYWORD,
        StarlarkTokenTypes.IN_KEYWORD,
        StarlarkTokenTypes.AND_KEYWORD,
        StarlarkTokenTypes.OR_KEYWORD,
        StarlarkTokenTypes.SEMICOLON,
      ),
    )

  /** `:`, and the tokens that end a line: a statement break and a dedent. */
  val COLON_SEARCH_STOPS =
    TokenSet.create(
      StarlarkTokenTypes.COLON,
      StarlarkTokenTypes.STATEMENT_BREAK,
      StarlarkTokenTypes.DEDENT,
    )
}
