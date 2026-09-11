package org.jetbrains.bazel.languages.starlark.utils

import org.jetbrains.annotations.ApiStatus
import java.nio.charset.StandardCharsets.UTF_8

@ApiStatus.Internal
object StarlarkStringDecoder {
  /**
   * Decodes a Starlark string literal value according to Bazel's lexer behavior:
   * https://github.com/bazelbuild/bazel/blob/6df8716a2497d1d084c2149b12719f8c4b7aff4e/src/main/java/net/starlark/java/syntax/Lexer.java
   *
   * This method returns the token value that Bazel's lexer would assign. It intentionally preserves
   * invalid escape sequences as '\' + escaped character, because Bazel reports a lexer error but still
   * stores that value in the STRING token.
   */
  fun decodeLiteral(text: String): String? {
    val literal = parseLiteral(text) ?: return null
    return decodeContent(
      literal.content,
      literal.quote.quote.first(),
      literal.isRaw,
      literal.quote.isTripleQuoted,
    )
  }

  private fun parseLiteral(text: String): StringLiteral? {
    val isRaw = text.startsWith('r') && text.getOrNull(1).isQuote()
    val quotedText = text.drop(if (isRaw) 1 else 0)
    val quote = StarlarkQuote.ofString(quotedText)

    if (quote == StarlarkQuote.UNQUOTED) return null
    if (quotedText.length < quote.quote.length * 2) return null
    if (!quotedText.endsWith(quote.quote)) return null

    return StringLiteral(quote.unwrap(quotedText), quote, isRaw)
  }

  private val StarlarkQuote.isTripleQuoted: Boolean
    get() = this == StarlarkQuote.TRIPLE_SINGLE || this == StarlarkQuote.TRIPLE_DOUBLE

  private fun Char?.isQuote(): Boolean = this == '\'' || this == '"'

  private data class StringLiteral(val content: String, val quote: StarlarkQuote, val isRaw: Boolean)

  private fun decodeContent(text: String, quote: Char, isRaw: Boolean, isTripleQuoted: Boolean): String {
    val literal = StringBuilder()
    var index = 0

    while (index < text.length) {
      val c = text[index]
      index++

      when (c) {
        '\n' -> if (isTripleQuoted) literal.appendByte('\n'.code) else return literal.toString()

        '\\' -> {
          if (index == text.length) return literal.toString()

          if (isRaw) {
            literal.appendByte('\\'.code)
            when {
              text.getOrNull(index) == '\r' && text.getOrNull(index + 1) == '\n' -> {
                literal.appendByte('\n'.code)
                index += 2
              }
              text[index] == '\r' || text[index] == '\n' -> {
                literal.appendByte('\n'.code)
                index += 1
              }
              else -> {
                index = literal.appendUtf8CodePoint(text, index)
              }
            }
          } else {
            val escaped = text[index]
            index++
            when (escaped) {
              '\r' -> if (text.getOrNull(index) == '\n') index += 1
              '\n' -> Unit
              'a' -> literal.appendByte(0x07)
              'b' -> literal.appendByte('\b'.code)
              'f' -> literal.appendByte('\u000C'.code)
              'n' -> literal.appendByte('\n'.code)
              'r' -> literal.appendByte('\r'.code)
              't' -> literal.appendByte('\t'.code)
              'v' -> literal.appendByte(0x0B)
              '\\' -> literal.appendByte('\\'.code)
              '\'' -> literal.appendByte('\''.code)
              '"' -> literal.appendByte('"'.code)
              in '0'..'7' -> {
                val octal = readOctalEscape(text, escaped, index)
                literal.appendByte(octal.value and BYTE_MAX_VALUE)
                index = octal.endOffset
              }
              else -> {
                literal.appendByte('\\'.code)
                index = literal.appendUtf8CodePoint(text, index - 1)
              }
            }
          }
        }

        '\'', '"' -> if (!isTripleQuoted && c == quote) return literal.toString() else index = literal.appendUtf8CodePoint(text, index - 1)

        else -> index = literal.appendUtf8CodePoint(text, index - 1)
      }
    }

    return literal.toString()
  }

  private fun StringBuilder.appendUtf8CodePoint(text: String, offset: Int): Int {
    val codePoint = Character.codePointAt(text, offset)
    String(Character.toChars(codePoint)).toByteArray(UTF_8).forEach { appendByte(it.toInt() and BYTE_MAX_VALUE) }
    return offset + Character.charCount(codePoint)
  }

  private fun StringBuilder.appendByte(value: Int) = append((value and BYTE_MAX_VALUE).toChar())

  private fun readOctalEscape(text: String, firstDigit: Char, offset: Int): OctalEscape {
    var value = firstDigit - '0'
    var index = offset
    repeat(MAX_OCTAL_ESCAPE_LENGTH - 1) {
      if (index < text.length && text[index] in '0'..'7') {
        value = (value shl OCTAL_SHIFT) or (text[index] - '0')
        index++
      }
    }
    return OctalEscape(value, index)
  }

  private data class OctalEscape(val value: Int, val endOffset: Int)

  private const val OCTAL_SHIFT = 3
  private const val MAX_OCTAL_ESCAPE_LENGTH = 3
  private const val BYTE_MAX_VALUE = 0xff
}
