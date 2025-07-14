package org.jetbrains.bazel.commons

internal class ProgramArgumentParser(private val input: String) {
  private var position = 0

  fun parse(): List<String> {
    val args = mutableListOf<String>()

    while (!isAtEnd()) {
      skipWhitespace()
      if (!isAtEnd()) {
        args.add(parseNextToken())
      }
    }

    return args
  }

  private fun parseNextToken(): String {
    return if (currentChar() == '"') {
      parseQuotedString()
    } else {
      parseUnquotedToken()
    }
  }

  private fun parseQuotedString(): String {
    advance()
    val result = StringBuilder()

    while (!isAtEnd() && currentChar() != '"') {
      if (currentChar() == '\\' && tryAdvance()) {
        result.append(currentChar())
        advance()
      } else {
        result.append(currentChar())
        advance()
      }
    }

    if (!isAtEnd()) {
      advance() // Skip closing quote
    }

    return result.toString()
  }

  private fun parseUnquotedToken(): String {
    val start = position

    while (!isAtEnd() && currentChar() != ' ' && currentChar() != '"') {
      advance()
    }

    return input.substring(start, position)
  }

  private fun skipWhitespace() {
    while (!isAtEnd() && currentChar() == ' ') {
      advance()
    }
  }

  private fun currentChar(): Char = input[position]

  private fun isAtEnd(): Boolean = position >= input.length

  private fun advance() {
    position++
  }

  private fun tryAdvance(): Boolean {
    if (this.position + 1 >= this.input.length) {
      return false
    }
    if (!isAtEnd()) {
      advance()
      return true
    }
    return false
  }
}

fun String.toProgramArguments() =
  ProgramArgumentParser(this.trim()).parse()
