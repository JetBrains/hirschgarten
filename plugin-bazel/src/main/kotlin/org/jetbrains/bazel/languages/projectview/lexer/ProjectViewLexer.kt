package org.jetbrains.bazel.languages.projectview.lexer

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewLexerBase.Token

/**
 * A lexer that extends [LexerBase] using [ProjectViewLexerBase] to tokenize text.
 *
 * @constructor Create an empty project view lexer.
 */
class ProjectViewLexer : LexerBase() {
  private var endOffset = 0
  private var offsetStart = 0
  private var buffer = ""
  private var tokens: Iterator<Token> = emptyList<Token>().iterator()
  private var currentToken: Token? = null

  override fun start(
    buffer: CharSequence,
    startOffset: Int,
    endOffset: Int,
    initialState: Int,
  ) {
    this.buffer = buffer.toString()
    this.offsetStart = startOffset
    this.endOffset = endOffset

    val lexer = ProjectViewLexerBase(buffer.subSequence(startOffset, endOffset))
    tokens = lexer.getTokens().iterator()
    currentToken = if (tokens.hasNext()) tokens.next() else null
  }

  /** ProjectViewLexer doesn't use states, so we return 0 as described in the Lexer docs. */
  override fun getState(): Int = 0

  override fun getTokenType(): IElementType? = currentToken?.type

  override fun getTokenStart(): Int = currentToken?.start?.plus(offsetStart) ?: 0

  override fun getTokenEnd(): Int = currentToken?.end?.plus(offsetStart) ?: 0

  override fun advance() {
    currentToken =
      if (currentToken != null && tokens.hasNext()) {
        tokens.next()
      } else {
        null
      }
  }

  override fun getBufferSequence(): CharSequence = buffer

  override fun getBufferEnd(): Int = endOffset
}
