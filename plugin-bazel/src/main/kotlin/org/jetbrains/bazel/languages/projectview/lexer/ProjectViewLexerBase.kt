package org.jetbrains.bazel.languages.projectview.lexer

class ProjectViewLexerBase(input: CharSequence) {
  private val tokens = mutableListOf<Token>()
  private var buffer = input.toString()
  private var pos = 0
  private var identifierStart: Int? = null
  private var lineHasPrecedingNonWhitespaceChar = false

  init {
    tokenize()
  }

  fun getTokens(): List<Token> = tokens

  private fun tokenize() {
    var c: Char

    while (pos < buffer.length) {
      c = buffer[pos]
      pos++

      when {
        c == '\n' -> {
          addPrecedingIdentifier(pos - 1)
          tokens.add(Token(ProjectViewTokenType.NEWLINE, pos - 1, pos))
          lineHasPrecedingNonWhitespaceChar = false
        }
        c.isWhitespace() -> {
          addPrecedingIdentifier(pos - 1)
          handleWhiteSpace()
        }
        c == ':' -> {
          addPrecedingIdentifier(pos - 1)
          tokens.add(Token(ProjectViewTokenType.COLON, pos - 1, pos))
        }
        c == '#' && !lineHasPrecedingNonWhitespaceChar -> {
          addPrecedingIdentifier(pos - 1)
          addCommentLine(pos - 1)
        }
        else -> {
          lineHasPrecedingNonWhitespaceChar = true
          if (identifierStart == null) {
            identifierStart = pos - 1
          }
        }
      }
    }

    addPrecedingIdentifier(pos)
  }

  private fun addPrecedingIdentifier(end: Int) {
    identifierStart?.let {
      tokens.add(Token(getIdentifierToken(it, end), it, end))
      identifierStart = null
    }
  }

  private fun addCommentLine(start: Int) {
    while (pos < buffer.length && buffer[pos] != '\n') {
      pos++
    }
    tokens.add(Token(ProjectViewTokenType.COMMENT, start, pos))
  }

  /**
   * If the whitespace is followed by an end-of-line comment or a newline, it's combined with those
   * tokens.
   */
  private fun handleWhiteSpace() {
    val oldPos = pos - 1
    when {
      pos >= buffer.length ||
        buffer[pos] == ' ' ||
        buffer[pos] == '\t' ||
        buffer[pos] == '\r' -> {
        if (pos < buffer.length) {
          pos++
        }
        tokens.add(Token(ProjectViewTokenType.WHITESPACE, oldPos, pos))
      }
      lineHasPrecedingNonWhitespaceChar || buffer[pos] == '#' || buffer[pos] == '\n' -> {
        tokens.add(Token(ProjectViewTokenType.WHITESPACE, oldPos, pos))
      }
      else -> {
        tokens.add(Token(ProjectViewTokenType.INDENT, oldPos, pos))
      }
    }
  }

  private fun getIdentifierToken(start: Int, end: Int): ProjectViewTokenType {
    val identifier = buffer.substring(start, end)
    // TODO: Import ProjectViewKeywords
    return when (identifier) {
      in ProjectViewKeywords.LIST_KEYWORD_MAP.keys -> ProjectViewTokenType.LIST_KEYWORD
      in ProjectViewKeywords.SCALAR_KEYWORD_MAP.keys -> ProjectViewTokenType.SCALAR_KEYWORD
      else -> ProjectViewTokenType.IDENTIFIER
    }
  }

  data class Token(
    val type: ProjectViewTokenType,
    val left: Int,
    val right: Int,
  )
}
