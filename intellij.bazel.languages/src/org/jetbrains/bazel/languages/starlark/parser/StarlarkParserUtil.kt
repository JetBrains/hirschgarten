package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.util.Key
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementType
import org.jetbrains.bazel.languages.starlark.elements.StarlarkElementTypes
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes

/** The external rules and the element type factory of `Starlark.bnf`. */
internal object StarlarkParserUtil : GeneratedParserUtilBase() {
  private val CHAIN_TRAILERS = TokenSet.create(StarlarkTokenTypes.DOT, StarlarkTokenTypes.LPAR, StarlarkTokenTypes.LBRACKET)

  /**
   * Removes the bracket pairs from the error state. With them, the error recovery skips a whole bracket group,
   * so after a stray token and an unclosed `(` it takes the rest of the file. The lexer ends an unclosed bracket at a line start,
   * and each recover rule names the brackets where it stops. Consumes nothing.
   */
  @JvmStatic
  fun withoutBracketGroups(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
    ErrorState.get(builder).braces = null
    return true
  }

  private val NO_STATEMENT_STARTS = TokenSet.orSet(
    StarlarkTokenSets.COMPOUND_ASSIGN_OPERATIONS,
    StarlarkTokenSets.CLOSE_BRACKETS,
    TokenSet.create(StarlarkTokenTypes.EQ, StarlarkTokenTypes.COMMA, StarlarkTokenTypes.COLON, StarlarkTokenTypes.DOT),
    TokenSet.create(StarlarkTokenTypes.ELSE_KEYWORD, StarlarkTokenTypes.ELIF_KEYWORD, StarlarkTokenTypes.IN_KEYWORD, StarlarkTokenTypes.AND_KEYWORD, StarlarkTokenTypes.OR_KEYWORD),
  )

  /** Tokens that never start or continue a statement. The next statement can start right after one of them. */
  private val STRAY_TOKENS = TokenSet.orSet(NO_STATEMENT_STARTS, TokenSet.create(StarlarkTokenTypes.SEMICOLON))

  private val STATEMENT_SEPARATORS = TokenSet.create(StarlarkTokenTypes.STATEMENT_BREAK, StarlarkTokenTypes.SEMICOLON)

  private val LINE_STARTS = TokenSet.create(
    StarlarkTokenTypes.STATEMENT_BREAK,
    StarlarkTokenTypes.INDENT,
    StarlarkTokenTypes.DEDENT,
  )

  private val INSIGNIFICANT_TOKENS = TokenSet.orSet(StarlarkTokenSets.WHITESPACE, StarlarkTokenSets.COMMENT)

  private val SPACES_TABS_AND_COMMENTS = TokenSet.orSet(StarlarkTokenSets.SPACES_AND_TABS, StarlarkTokenSets.COMMENT)

  private val COLON_SEARCH_STOPS = TokenSet.create(
    StarlarkTokenTypes.COLON,
    StarlarkTokenTypes.STATEMENT_BREAK,
    StarlarkTokenTypes.DEDENT,
  )

  /** The offset where a statement starts after a statement error. */
  private val STATEMENT_START_OFFSET = Key.create<Int>("starlark.parser.statement.start.offset")

  private val TARGET_MODE = Key.create<Boolean>("starlark.parser.target.mode")


  /**
   * Consumes the `,` between two items of a list. When the `,` is missing and [start] can start the next item,
   * reports the missing `,`, and the list goes on with that item.
   */
  @JvmStatic
  fun commaSeparator(builder: PsiBuilder, level: Int, start: Parser): Boolean {
    if (builder.tokenType === StarlarkTokenTypes.COMMA) {
      builder.advanceLexer()
      return true
    }
    if (!lookahead(builder, level, start)) return false
    builder.error(StarlarkBundle.message("parser.expected.comma"))
    return true
  }

  /**
   * Reports a missing simple statement after `:`. A token that cannot start a statement goes into the error.
   * The next statement starts after the error, as after [statementEnd].
   */
  @JvmStatic
  fun statementExpected(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
    val type = builder.tokenType
    if (type == null) {
      builder.error(StarlarkBundle.message("parser.indent.expected"))
    } else if (type in STRAY_TOKENS) {
      val unexpected = builder.mark()
      builder.advanceLexer()
      unexpected.error(StarlarkBundle.message("parser.statement.expected.found.0", type.toString()))
    } else {
      builder.error(StarlarkBundle.message("parser.statement.expected.found.0", type.toString()))
    }
    startStatementHere(builder)
    return true
  }

  @JvmStatic
  fun indentExpected(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
    builder.error(StarlarkBundle.message("parser.indent.expected"))
    return true
  }

  @JvmStatic
  fun elementType(name: String): IElementType {
    // Reading one property creates all the element types of the object.
    StarlarkElementTypes.STATEMENT_LIST
    return IElementType.enumerate { it is StarlarkElementType && it.toString() == name }.single()
  }

  /** True when [parser] matches at the current token. Unlike `&`, it adds no expected tokens to the error messages. */
  @JvmStatic
  fun lookahead(builder: PsiBuilder, level: Int, parser: Parser): Boolean {
    // In two nested predicates, Grammar-Kit records no expected tokens.
    val outer = enter_section_(builder, level, _AND_)
    val inner = enter_section_(builder, level + 1, _AND_)
    val result = parser.parse(builder, level + 2)
    exit_section_(builder, level + 1, inner, result, false, null)
    exit_section_(builder, level, outer, result, false, null)
    return result
  }

  /** Parses [parser] in target mode. Each plain name in it is a target. */
  @JvmStatic
  fun targetMode(builder: PsiBuilder, level: Int, parser: Parser): Boolean = withFlag(builder, TARGET_MODE, true) { parser.parse(builder, level) }


  /** Parses a member chain. In target mode, a chain with a trailer is a normal expression: `a.b = 1` binds no name. */
  @JvmStatic
  fun memberChain(builder: PsiBuilder, level: Int, chain: Parser): Boolean {
    if (builder.getUserData(TARGET_MODE) != true) return chain.parse(builder, level)
    val ahead = Lookahead(builder)
    val hasTrailer = ahead.skipPrimary() && ahead.peek() in CHAIN_TRAILERS
    return withFlag(builder, TARGET_MODE, !hasTrailer) { chain.parse(builder, level) }
  }

  /** Parses a name. In target mode, it is a target, except `_`. */
  @JvmStatic
  fun nameExpression(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
    if (builder.tokenType !== StarlarkTokenTypes.IDENTIFIER) return false
    val isTarget = builder.getUserData(TARGET_MODE) == true && builder.tokenText != "_"
    doneToken(builder, if (isTarget) StarlarkElementTypes.TARGET_EXPRESSION else StarlarkElementTypes.REFERENCE_EXPRESSION)
    return true
  }

  @JvmStatic
  fun atAssignment(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean =
    findStatementOperator(builder) === StarlarkTokenTypes.EQ

  @JvmStatic
  fun atAugAssignment(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean =
    findStatementOperator(builder) in StarlarkTokenSets.COMPOUND_ASSIGN_OPERATIONS

  /**
   * Ends a simple statement. On an error, the next statement starts at the current token if a statement can start there.
   * If not, as for the second '=' of `a = b = 1`, the error covers the rest of the statement up to the line end or `;`.
   * A compound statement can start only on a new line, so a `for` in the middle of `[a, b for b in c]` is in that error too.
   */
  @JvmStatic
  fun statementEnd(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
    when {
      builder.tokenType === StarlarkTokenTypes.STATEMENT_BREAK -> builder.advanceLexer()
      builder.tokenType === StarlarkTokenTypes.SEMICOLON || builder.eof() -> Unit
      builder.tokenType in NO_STATEMENT_STARTS ||
      builder.tokenType in StarlarkTokenSets.COMPOUND_STATEMENT_KEYWORDS && !followsLineBreak(builder) -> {
        val rest = builder.mark()
        while (!builder.eof() && builder.tokenType !in STATEMENT_SEPARATORS) {
          builder.advanceLexer()
        }
        rest.error(StarlarkBundle.message("parser.end.of.statement.expected"))
        if (builder.tokenType === StarlarkTokenTypes.STATEMENT_BREAK) builder.advanceLexer()
      }
      else -> {
        builder.error(StarlarkBundle.message("parser.end.of.statement.expected"))
        startStatementHere(builder)
      }
    }
    return true
  }

  /** Consumes the `:` before a suite, and reports the unexpected tokens before it as one error. */
  @JvmStatic
  fun expectColon(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
    if (builder.tokenType === StarlarkTokenTypes.COLON) {
      builder.advanceLexer()
      return true
    }
    if (builder.tokenType === StarlarkTokenTypes.STATEMENT_BREAK || builder.eof()) {
      builder.error(StarlarkBundle.message("parser.expected.colon"))
      return true
    }
    val unexpected = builder.mark()
    while (!builder.eof() && builder.tokenType !in COLON_SEARCH_STOPS) {
      builder.advanceLexer()
    }
    if (builder.tokenType === StarlarkTokenTypes.COLON) {
      builder.advanceLexer()
    }
    unexpected.error(StarlarkBundle.message("parser.expected.colon"))
    return true
  }

  /** True when the current token is the first token of a line, or of a statement after an [statementEnd] error. */
  @JvmStatic
  fun atLineStart(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
    builder.tokenType // Skips the whitespace before the current token.
    if (builder.getUserData(STATEMENT_START_OFFSET) == builder.currentOffset) return true
    return followsLineStart(builder)
  }

  /** True when the current token is on the same physical line as the token before it. */
  @JvmStatic
  fun atSameLine(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
    val type = builder.tokenType ?: return false
    return type !in LINE_STARTS && !followsLineBreak(builder)
  }

  private fun followsLineStart(builder: PsiBuilder): Boolean {
    var step = -1
    while (true) {
      val type = builder.rawLookup(step) ?: return true
      if (type !in INSIGNIFICANT_TOKENS) return type in LINE_STARTS
      step--
    }
  }

  @JvmStatic
  fun unexpectedIndent(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
    if (builder.tokenType !== StarlarkTokenTypes.INDENT) return false
    val indent = builder.mark()
    builder.advanceLexer()
    indent.error(StarlarkBundle.message("parser.unexpected.indent"))
    return true
  }

  @JvmStatic
  fun unexpectedStatementToken(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
    val type = builder.tokenType ?: return false
    val unexpected = builder.mark()
    builder.advanceLexer()
    val message = when (type) {
      StarlarkTokenTypes.INCONSISTENT_DEDENT -> StarlarkBundle.message("parser.unindent.does.not.match.any.outer.indent")
      else -> StarlarkBundle.message("parser.statement.expected.found.0", type.toString())
    }
    unexpected.error(message)
    if (type in STRAY_TOKENS) {
      startStatementHere(builder)
    }
    return true
  }

  /** True when a line break comes before the current token, also inside brackets, where the lexer makes no statement break. */
  private fun followsLineBreak(builder: PsiBuilder): Boolean {
    var step = -1
    while (true) {
      val type = builder.rawLookup(step) ?: return true
      if (type !in SPACES_TABS_AND_COMMENTS) return type === StarlarkTokenTypes.LINE_BREAK || type in LINE_STARTS
      step--
    }
  }

  private fun startStatementHere(builder: PsiBuilder) {
    builder.tokenType // Skips the whitespace before the current token.
    builder.putUserData(STATEMENT_START_OFFSET, builder.currentOffset)
  }

  /** Returns the first `=` or augmented assignment operator of the statement outside of brackets. */
  private fun findStatementOperator(builder: PsiBuilder): IElementType? {
    val ahead = Lookahead(builder)
    while (true) {
      val type = ahead.nextOutsideBrackets() ?: return null
      when {
        // A '=' after a lambda gives a default value of the lambda: `lambda a = 1: a` is no assignment.
        type === StarlarkTokenTypes.LAMBDA_KEYWORD -> return null
        type === StarlarkTokenTypes.EQ || type in StarlarkTokenSets.COMPOUND_ASSIGN_OPERATIONS -> return type
        type in StarlarkTokenSets.ENDS_OF_STATEMENT -> return null
      }
    }
  }

  private inline fun <T> withFlag(builder: PsiBuilder, key: Key<Boolean>, value: Boolean, action: () -> T): T {
    val previous = builder.getUserData(key)
    builder.putUserData(key, value)
    try {
      return action()
    }
    finally {
      builder.putUserData(key, previous)
    }
  }

  /** Reads the tokens ahead without moving the builder, and skips whitespace and comments. */
  private class Lookahead(private val builder: PsiBuilder) {
    private var step = 0

    init {
      builder.tokenType // Skips the whitespace before the current token.
    }

    fun peek(): IElementType? {
      while (true) {
        val type = builder.rawLookup(step) ?: return null
        if (type !in INSIGNIFICANT_TOKENS) return type
        step++
      }
    }

    fun advance() {
      peek()
      step++
    }

    /** Skips a token, or brackets with their content. */
    fun skipPrimary(): Boolean {
      val type = peek() ?: return false
      if (type in StarlarkTokenSets.OPEN_BRACKETS) return skipBrackets()
      advance()
      return true
    }

    fun skipBrackets(): Boolean {
      var depth = 0
      while (true) {
        val type = peek() ?: return false
        advance()
        when {
          type in StarlarkTokenSets.OPEN_BRACKETS -> depth++
          type in StarlarkTokenSets.CLOSE_BRACKETS -> if (--depth == 0) return true
          // The lexer gives no STATEMENT_BREAK in brackets, except at the end of the file.
          type === StarlarkTokenTypes.STATEMENT_BREAK -> return false
        }
      }
    }

    /** Returns the next token outside of brackets, or null at the end of the enclosing brackets or of the file. */
    fun nextOutsideBrackets(): IElementType? {
      while (true) {
        val type = peek() ?: return null
        when {
          type in StarlarkTokenSets.OPEN_BRACKETS -> if (!skipBrackets()) return null
          type in StarlarkTokenSets.CLOSE_BRACKETS -> return null
          else -> {
            advance()
            return type
          }
        }
      }
    }
  }

  private fun doneToken(builder: PsiBuilder, type: IElementType) {
    val marker = builder.mark()
    builder.advanceLexer()
    marker.done(type)
  }
}
