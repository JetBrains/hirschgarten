package org.jetbrains.bazel.languages.starlark.lexer

import com.intellij.lexer.FlexAdapter
import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenSets
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes

/**
 * The lexer adds these tokens to the tokens of the JFlex lexer:
 * - a statement break at the end of each logical line with a significant token,
 * - an indent when a line opens a block, and a dedent for each block that a line closes,
 * - one line break token for a line break and the surrounding whitespace.
 *
 * A line break inside brackets does not end the logical line, and the indent of the next line does not count.
 * A bracket without a pair ends at the line break before a line that cannot be inside brackets in valid code.
 * See [LexerState.endsBrackets].
 *
 * For `if a:⏎    b⏎c` the tokens are `if a : STATEMENT_BREAK ⏎ INDENT b STATEMENT_BREAK DEDENT ⏎ c STATEMENT_BREAK`.
 * The whitespace inside a line, as in `if a`, stays a separate token.
 *
 * The lexer reads the JFlex tokens once. At a line break, it reads ahead to the next significant token.
 * A significant token is a token that is not whitespace and not a comment.
 * [getState] does not hold the [LexerState], so the lexer must start at offset 0.
 */
@ApiStatus.Internal
class StarlarkIndentingLexer : LexerBase() {
  private val base = FlexAdapter(_StarlarkLexer(null))
  private var buffer: CharSequence = ""
  private var bufferEnd = 0
  private var state = LexerState()

  /** The start of the base token, or the text end after the last token. */
  private val baseStart: Int get() = if (base.tokenType == null) bufferEnd else base.tokenStart

  private val baseEnd: Int get() = if (base.tokenType == null) bufferEnd else base.tokenEnd

  override fun getTokenType(): IElementType? = state.pending.first?.type ?: base.tokenType

  override fun getTokenStart(): Int = state.pending.first?.start ?: baseStart

  override fun getTokenEnd(): Int = state.pending.first?.end ?: baseEnd

  override fun getState(): Int = 0

  override fun getBufferSequence(): CharSequence = buffer

  override fun getBufferEnd(): Int = bufferEnd

  override fun start(
    buffer: CharSequence,
    startOffset: Int,
    endOffset: Int,
    initialState: Int,
  ) {
    require(startOffset == 0) { "StarlarkIndentingLexer starts only at offset 0, not at $startOffset" }
    this.buffer = buffer
    bufferEnd = endOffset
    state = LexerState()
    base.start(buffer, 0, endOffset, 0)
    processLineBreaks(start = 0)
    base.tokenType?.let(state::trackSignificantToken)
  }

  override fun advance() {
    if (state.pending.tryAdvance() || base.tokenType == null) return
    base.advance()
    when (base.tokenType) {
      null, StarlarkTokenTypes.LINE_BREAK -> processLineBreaks(baseStart)
      in StarlarkTokenSets.SPACES_AND_TABS -> processSpaces()
    }
    base.tokenType?.let(state::trackSignificantToken)
  }

  /** Processes the spaces and tabs in a line. If a `⏎` or the text end follows, it calls [processLineBreaks]. */
  private fun processSpaces() {
    val start = baseStart
    val spaces = ArrayList<PendingToken>()
    while (base.tokenType in StarlarkTokenSets.SPACES_AND_TABS) {
      spaces.add(PendingToken(base.tokenType!!, base.tokenStart, base.tokenEnd))
      base.advance()
    }
    val type = base.tokenType
    when {
      type == null || type === StarlarkTokenTypes.LINE_BREAK -> processLineBreaks(start)
      else -> state.pending.addAll(spaces)
    }
  }

  /** Processes the line breaks and the comment lines between them, up to the next significant token or the text end. */
  private fun processLineBreaks(start: Int) {
    val lineBreaks = arrayListOf(readLineBreak(start))
    val comments = ArrayList<PendingCommentToken>()
    while (base.tokenType === StarlarkTokenTypes.COMMENT) {
      comments.add(PendingCommentToken(base.tokenStart, base.tokenEnd, lineBreaks.last().nextIndent))
      base.advance()
      lineBreaks.add(readLineBreak(baseStart))
    }
    state.trackLineBreaks(lineBreaks = lineBreaks, comments = comments, next = base.tokenType)
  }

  /**
   * Reads all the whitespace from [start]: the spaces at the end of a line, the `⏎`, the blank lines and the indent.
   * [LineBreak.nextIndent] is the indent of the line after the last `⏎`.
   * The indent is the spaces and tabs at the start of a line. A line continuation stops the count.
   */
  private fun readLineBreak(start: Int): LineBreak {
    var indent = 0
    var holdsLineBreak = false
    while (base.tokenType in StarlarkTokenSets.WHITESPACE) {
      val type = base.tokenType
      val length = base.tokenEnd - base.tokenStart
      base.advance()
      when (type) {
        StarlarkTokenTypes.LINE_BREAK -> {
          holdsLineBreak = true
          indent = 0
        }
        StarlarkTokenTypes.TAB -> indent = nextTabStop(indent)
        StarlarkTokenTypes.SPACE -> indent += length
        StarlarkTokenTypes.LINE_CONTINUATION -> {
          holdsLineBreak = true
          skipSpacesAndTabs()
        }
      }
    }
    return LineBreak(
      start = start,
      end = baseStart,
      whitespaceType = when {
        holdsLineBreak -> StarlarkTokenTypes.LINE_BREAK
        else -> StarlarkTokenTypes.SPACE
      },
      nextIndent = indent,
    )
  }

  private fun skipSpacesAndTabs() {
    while (base.tokenType in StarlarkTokenSets.SPACES_AND_TABS) base.advance()
  }
}

/** An open bracket of [type]. [lineIndent] is the indent of the line where it opens. */
private class OpenBracket(val type: IElementType, val lineIndent: Int)

/**
 * All the whitespace between the last token of a line and the first token of the next line, from [start] to [end].
 * For `x = 1  ⏎⏎    y`, the line break is `  ⏎⏎    `. At the text start and at the text end, it can be empty.
 * [nextIndent] is the indent of the line after it.
 */
private class LineBreak(val start: Int, val end: Int, val whitespaceType: IElementType, val nextIndent: Int) {
  /** Returns the whitespace token of the line break, or null if the line break is empty. */
  fun toToken(): PendingToken? = if (start < end) PendingToken(whitespaceType, start, end) else null
}

private class LexerState {

  val indents = IndentStack()

  /** The open brackets, outermost first. Empty outside brackets. */
  val brackets = ArrayDeque<OpenBracket>()

  /** The tokens that the lexer made but did not return yet. */
  val pending = PendingTokens()

  /** The indent of the current line. */
  var lineIndent = 0

  /** The first significant token of the logical line. It is null before the first significant token of the text. */
  var firstSignificantType: IElementType? = null

  var lastSignificantType: IElementType? = null
}

private val LexerState.isInsideBrackets: Boolean get() = brackets.isNotEmpty()

/**
 * True if the open brackets end before the [next] significant token, because the next line cannot be inside brackets in valid code:
 * - the text ends,
 * - the next line starts with a keyword that cannot be inside brackets, such as `def` or `return`,
 * - the next line starts with an operand right after an expression end, as `y` in `x = f(a⏎y = 2`.
 *   It must not be indented more than the line of the outermost bracket, unless the logical line is a compound header,
 *   as in `def f(a, b⏎    x = 1`.
 *
 * It is false if no bracket is open.
 */
private fun LexerState.endsBrackets(next: IElementType?): Boolean {
  val outermost = brackets.firstOrNull() ?: return false
  if (next == null || next in StarlarkTokenSets.KEYWORDS_OUTSIDE_BRACKETS) return true
  if (!next.isOperandAfter(lastSignificantType)) return false
  return lineIndent <= outermost.lineIndent || firstSignificantType in StarlarkTokenSets.COMPOUND_STATEMENT_KEYWORDS
}

/**
 * Moves the state over a run of [lineBreaks] and the [comments] between them, to the [next] significant token.
 * It ends the open brackets if [next] cannot be inside them. Outside brackets, the next logical line starts with [next].
 * The tokens of the run go to [LexerState.pending] in this order: the statement break, the line breaks and the comments,
 * then the dedents, the whitespace and the indent of the last line break.
 */
private fun LexerState.trackLineBreaks(
  lineBreaks: List<LineBreak>,
  comments: List<PendingCommentToken>,
  next: IElementType?,
) {
  lineIndent = if (next == null) 0 else lineBreaks.last().nextIndent
  if (endsBrackets(next)) brackets.clear()
  if (isInsideBrackets) {
    addLineBreaks(lineBreaks, comments, change = null)
    return
  }
  if (firstSignificantType != null) {
    val offset = lineBreaks.first().start
    pending.add(PendingToken(StarlarkTokenTypes.STATEMENT_BREAK, offset, offset))
  }
  firstSignificantType = next
  addLineBreaks(lineBreaks, comments, indents.changeTo(lineIndent))
}

private fun LexerState.trackSignificantToken(type: IElementType) {
  if (type in StarlarkTokenSets.WHITESPACE_AND_COMMENTS) return
  when (type) {
    in StarlarkTokenSets.OPEN_BRACKETS -> brackets.addLast(OpenBracket(type, lineIndent))
    in StarlarkTokenSets.CLOSE_BRACKETS -> closeBracket(type)
  }
  lastSignificantType = type
}

/** Closes the nearest open bracket of the kind of [closer], and the brackets inside it. A closer with no such bracket closes nothing. */
private fun LexerState.closeBracket(closer: IElementType) {
  val position = brackets.indexOfLast { it.type.pairsWith(closer) }
  if (position < 0) return
  while (brackets.size > position) brackets.removeLast()
}

/** Adds the line breaks and the comments. At the last line break, adds the dedents, then the whitespace, then the indent of [change]. */
private fun LexerState.addLineBreaks(
  lineBreaks: List<LineBreak>,
  comments: List<PendingCommentToken>,
  change: IndentStack.Change?,
) {
  comments.forEachIndexed { i, comment ->
    lineBreaks[i].toToken()?.let(pending::add)
    pending.add(comment)
  }
  val last = lineBreaks.last()
  if (change is IndentStack.Change.Dedented) pending.addDedents(change.closedLevels, change.isInconsistent, last.start)
  last.toToken()?.let(pending::add)
  if (change is IndentStack.Change.Indented) pending.addIndent(change.indent, last.end)
}

private fun IElementType.isOperandAfter(lastType: IElementType?): Boolean =
  this in StarlarkTokenSets.OPERAND_STARTS &&
  lastType in StarlarkTokenSets.EXPRESSION_ENDS &&
  !(this in StarlarkTokenSets.STRINGS && lastType in StarlarkTokenSets.STRINGS)

private fun IElementType.pairsWith(closer: IElementType): Boolean =
  when (this) {
    StarlarkTokenTypes.LPAR -> closer === StarlarkTokenTypes.RPAR
    StarlarkTokenTypes.LBRACKET -> closer === StarlarkTokenTypes.RBRACKET
    StarlarkTokenTypes.LBRACE -> closer === StarlarkTokenTypes.RBRACE
    else -> false
  }

/** A tab moves the indent to the next multiple of 8, as in Python. */
private fun nextTabStop(indent: Int): Int = (indent / 8 + 1) * 8
