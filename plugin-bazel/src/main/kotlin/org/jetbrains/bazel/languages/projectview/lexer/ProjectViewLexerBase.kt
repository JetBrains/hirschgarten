package org.jetbrains.bazel.languages.projectview.lexer

/**
 * A base class responsible for tokenizing a given [input] project view into a list of tokens.
 *
 * @constructor Creates a lexer which returns a list of tokens for the given [input].
 * @param input The input to tokenize.
 */
class ProjectViewLexerBase(input: CharSequence) {
  /**
   * A sequence of characters that are neither whitespace nor colon
   * and is not a part of a comment.
   */
  private data class Identifier(val start: Int, val tokenType: ProjectViewTokenType)

  private val tokens = mutableListOf<Token>()
  private var buffer = input.toString()
  private var pos = 0
  private var currentIdentifier: Identifier? = null
  private var isPosAfterNonWhitespaceCharInLine = false

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
          isPosAfterNonWhitespaceCharInLine = false
        }
        isHorizontalWhitespace(c) -> {
          addPrecedingIdentifier(pos - 1)
          handleWhiteSpace()
        }
        c == ':' -> {
          addPrecedingIdentifier(pos - 1)
          tokens.add(Token(ProjectViewTokenType.COLON, pos - 1, pos))
        }
        c == '#' && !isPosAfterNonWhitespaceCharInLine -> {
          addPrecedingIdentifier(pos - 1)
          addCommentLine(pos - 1)
        }
        else -> {
          isPosAfterNonWhitespaceCharInLine = true
          if (currentIdentifier == null) {
            // The identifier is a keyword if it's at the beginning of the line.
            val tokenType =
              if ((pos == 1) || (buffer[pos - 2] == '\n')) {
                ProjectViewTokenType.KEYWORD
              } else {
                ProjectViewTokenType.IDENTIFIER
              }
            currentIdentifier = Identifier(pos - 1, tokenType)
          }
        }
      }
    }

    addPrecedingIdentifier(pos)
  }

  private fun addPrecedingIdentifier(end: Int) {
    currentIdentifier?.let {
      tokens.add(Token(it.tokenType, it.start, end))
      currentIdentifier = null
    }
  }

  private fun addCommentLine(start: Int) {
    while (pos < buffer.length && buffer[pos] != '\n') {
      pos++
    }
    tokens.add(Token(ProjectViewTokenType.COMMENT, start, pos))
  }

  private fun handleWhiteSpace() {
    // Every whitespace followed by a single-line comment or a newline is fused with the following token.
    val oldPos = pos - 1
    while (pos < buffer.length) {
      if (isHorizontalWhitespace(buffer[pos])) {
        pos++
        continue
      }

      if (isPosAfterNonWhitespaceCharInLine || buffer[pos] == '#' || buffer[pos] == '\n') {
        tokens.add(Token(ProjectViewTokenType.WHITESPACE, oldPos, pos))
      } else {
        tokens.add(Token(ProjectViewTokenType.INDENT, oldPos, pos))
      }

      return
    }

    tokens.add(Token(ProjectViewTokenType.WHITESPACE, oldPos, pos))
  }

  private fun isHorizontalWhitespace(c: Char): Boolean = c == ' ' || c == '\t' || c == '\r'

  /**
   * Represents a token in the project view file.
   *
   * @property type The type of the token.
   * @property start Position of the first character of the token in the buffer passed to the [ProjectViewLexerBase] constructor.
   * @property end Sum of [start] and the length of the token.
   */
  data class Token(
    val type: ProjectViewTokenType,
    val start: Int,
    val end: Int,
  )
}
