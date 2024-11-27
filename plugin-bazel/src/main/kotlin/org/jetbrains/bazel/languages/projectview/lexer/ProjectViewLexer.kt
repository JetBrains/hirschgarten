package org.jetbrains.bazel.languages.projectview.lexer

import com.intellij.lexer.LexerBase
import com.intellij.psi.tree.IElementType

class ProjectViewLexer: LexerBase() {

  private var endOffset = 0
  private var offsetStart = 0
  private var buffer = ""
  private var tokens: Iterator<ProjectViewLexerBase.Token> =
    emptyList<ProjectViewLexerBase.Token>().iterator()
  private var currentToken: ProjectViewLexerBase.Token? = null

  override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
    this.buffer = buffer.toString()
    this.offsetStart = startOffset
    this.endOffset = endOffset

    var lexer: ProjectViewLexerBase =
      ProjectViewLexerBase(buffer.subSequence(startOffset, endOffset))
    tokens = lexer.getTokens().iterator()
    currentToken = if (tokens.hasNext()) tokens.next() else null
  }

  override fun getState(): Int = 0

  override fun getTokenType(): IElementType? = currentToken?.type

  override fun getTokenStart(): Int {
    if (currentToken == null) {
      return 0
    }
    return currentToken!!.left + offsetStart
  }

  override fun getTokenEnd(): Int {
    if (currentToken == null) {
      return 0
    }
    return currentToken!!.right + offsetStart
  }

  override fun advance() {
    if (currentToken != null && tokens.hasNext()) {
      currentToken = tokens.next()
    }
    else {
      currentToken = null
    }
  }

  override fun getBufferSequence(): CharSequence = buffer

  override fun getBufferEnd(): Int = endOffset
}
