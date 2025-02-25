package org.jetbrains.bazel.languages.projectview.lexer

import org.jetbrains.bazel.languages.projectview.elements.ProjectViewTokenType
import org.jetbrains.bazel.languages.projectview.elements.ProjectViewTokenTypes

/**
 * A base class responsible for tokenizing a given [input] project view into a list of tokens.
 *
 * @constructor Creates a lexer which returns a list of tokens for the given [input].
 * @param input The input to tokenize.
 */
class ProjectViewLexerBase(input: CharSequence) {
  private val tokens = mutableListOf<Token>()
  private var buffer = input.toString()
  private var pos = 0
  private var identifierStart: Int? = null
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
          tokens.add(Token(ProjectViewTokenTypes.NEWLINE, pos - 1, pos))
          isPosAfterNonWhitespaceCharInLine = false
        }
        isHorizontalWhitespace(c) -> {
          addPrecedingIdentifier(pos - 1)
          handleWhiteSpace()
        }
        c == ':' -> {
          addPrecedingIdentifier(pos - 1)
          tokens.add(Token(ProjectViewTokenTypes.COLON, pos - 1, pos))
        }
        c == '#' && !isPosAfterNonWhitespaceCharInLine -> {
          addPrecedingIdentifier(pos - 1)
          addCommentLine(pos - 1)
        }
        else -> {
          isPosAfterNonWhitespaceCharInLine = true
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
    tokens.add(Token(ProjectViewTokenTypes.COMMENT, start, pos))
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
        tokens.add(Token(ProjectViewTokenTypes.WHITESPACE, oldPos, pos))
      } else {
        tokens.add(Token(ProjectViewTokenTypes.INDENT, oldPos, pos))
      }

      return
    }

    tokens.add(Token(ProjectViewTokenTypes.WHITESPACE, oldPos, pos))
  }

  private fun getIdentifierToken(start: Int, end: Int): ProjectViewTokenType {
    val identifier = buffer.substring(start, end)
    return when (identifier) {
      in ProjectViewTokenTypes.SCALAR_KEYWORDS_SET -> ProjectViewTokenTypes.SCALAR_KEYWORD
      in ProjectViewTokenTypes.LIST_KEYWORDS_SET -> ProjectViewTokenTypes.LIST_KEYWORD
      else -> ProjectViewTokenTypes.IDENTIFIER
    }
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
