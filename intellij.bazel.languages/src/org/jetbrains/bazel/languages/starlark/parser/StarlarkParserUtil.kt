package org.jetbrains.bazel.languages.starlark.parser

import com.intellij.lang.PsiBuilder
import com.intellij.lang.parser.GeneratedParserUtilBase
import com.intellij.openapi.util.Key
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets
import org.jetbrains.bazel.languages.starlark.parser.generated.StarlarkTokenTypes

/** The external rules of `Starlark.bnf`. */
internal object StarlarkParserUtil : GeneratedParserUtilBase() {

  private val STATEMENT_START_OFFSET = Key.create<Int>("starlark.parser.statement.start.offset")

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

  @JvmStatic
  fun missingComma(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
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
    when (type) {
      in StarlarkTokenSets.NO_STATEMENT_STARTS -> {
        val unexpected = builder.mark()
        builder.advanceLexer()
        unexpected.error(StarlarkBundle.message("parser.statement.expected.found.0", type.toString()))
      }
      null -> builder.error(StarlarkBundle.message("parser.indent.expected"))
      else -> builder.error(StarlarkBundle.message("parser.statement.expected.found.0", type.toString()))
    }
    startStatementHere(builder)
    return true
  }

  @JvmStatic
  fun indentExpected(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
    builder.error(StarlarkBundle.message("parser.indent.expected"))
    return true
  }

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
      builder.tokenType in StarlarkTokenSets.NO_STATEMENT_STARTS ||
      builder.tokenType in StarlarkTokenSets.COMPOUND_STATEMENT_KEYWORDS && !followsLineBreak(builder) -> {
        val rest = builder.mark()
        while (!builder.eof() && builder.tokenType !in StarlarkTokenSets.STATEMENT_SEPARATORS) {
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
    while (!builder.eof() && builder.tokenType !in StarlarkTokenSets.COLON_SEARCH_STOPS) {
      builder.advanceLexer()
    }
    if (builder.tokenType === StarlarkTokenTypes.COLON) {
      builder.advanceLexer()
    }
    unexpected.error(StarlarkBundle.message("parser.expected.colon"))
    return true
  }

  /** True when the current token is the first token of a logical line, or of a statement after a [statementEnd] error. */
  @JvmStatic
  fun atLineStart(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
    builder.tokenType // Skips the whitespace before the current token.
    return builder.getUserData(STATEMENT_START_OFFSET) == builder.currentOffset || followsLineStart(builder)
  }

  /** True when the current token is on the same physical line as the token before it. */
  @JvmStatic
  fun atSameLine(builder: PsiBuilder, @Suppress("UNUSED_PARAMETER") level: Int): Boolean {
    val type = builder.tokenType ?: return false
    return type !in StarlarkTokenSets.LINE_STARTS && !followsLineBreak(builder)
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
    if (type in StarlarkTokenSets.NO_STATEMENT_STARTS) {
      startStatementHere(builder)
    }
    return true
  }

  /** True when a statement break, an indent or a dedent comes before the current token, so a logical line starts here. */
  private fun followsLineStart(builder: PsiBuilder): Boolean {
    var step = -1
    while (true) {
      val type = builder.rawLookup(step) ?: return true
      if (type !in StarlarkTokenSets.WHITESPACE_AND_COMMENTS) return type in StarlarkTokenSets.LINE_STARTS
      step--
    }
  }

  /** True when a line break comes before the current token, also inside brackets, where the lexer makes no statement break. */
  private fun followsLineBreak(builder: PsiBuilder): Boolean {
    var step = -1
    while (true) {
      val type = builder.rawLookup(step) ?: return true
      if (type !in StarlarkTokenSets.SPACES_TABS_AND_COMMENTS) return type === StarlarkTokenTypes.LINE_BREAK || type in StarlarkTokenSets.LINE_STARTS
      step--
    }
  }

  private fun startStatementHere(builder: PsiBuilder) {
    builder.tokenType // Skips the whitespace before the current token.
    builder.putUserData(STATEMENT_START_OFFSET, builder.currentOffset)
  }
}
