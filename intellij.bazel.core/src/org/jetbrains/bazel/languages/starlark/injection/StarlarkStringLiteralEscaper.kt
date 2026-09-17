package org.jetbrains.bazel.languages.starlark.injection

import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.utils.StarlarkQuote

/**
 * Maps between the source text of a Starlark string literal and its decoded value, so that
 * languages injected into the literal see the actual string value and can map offsets back.
 *
 * Handles the escape sequences supported by Starlark: `\n`, `\t`, `\r`, `\a`, `\b`, `\f`, `\v`,
 * `\\`, `\"`, `\'`, `\<newline>` (line continuation), `\ooo`, `\xhh`, `\uhhhh` and `\Uhhhhhhhh`.
 * Unknown escape sequences are kept as-is. Raw strings (`r"..."`) are not decoded at all.
 *
 * The companion also provides the inverse operation, [escape], which is used when the contents of a literal are
 * replaced.
 */
internal class StarlarkStringLiteralEscaper(host: StarlarkStringLiteralExpression) : LiteralTextEscaper<StarlarkStringLiteralExpression>(host) {
  /** For every offset in the decoded text, the corresponding offset in the (sub-)text of the host. */
  private var outSourceOffsets: IntArray = IntArray(0)
  private var decodedLength: Int = 0

  override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
    val subText = rangeInsideHost.substring(myHost.text)
    val outStart = outChars.length
    outSourceOffsets = IntArray(subText.length + 1)
    if (myHost.isRaw()) {
      outChars.append(subText)
      for (i in 0..subText.length) outSourceOffsets[i] = i
    } else {
      decodeEscapes(subText, outChars, outSourceOffsets)
    }
    decodedLength = outChars.length - outStart
    return true
  }

  override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
    if (offsetInDecoded < 0 || offsetInDecoded > decodedLength) return -1
    val result = outSourceOffsets[offsetInDecoded]
    return rangeInsideHost.startOffset + minOf(result, rangeInsideHost.length)
  }

  override fun isOneLine(): Boolean = !myHost.getQuote().isTriple

  companion object {
    private const val BELL = '\u0007'
    private const val BACKSPACE = '\u0008'
    private const val FORM_FEED = '\u000C'
    private const val VERTICAL_TAB = '\u000B'

    /**
     * Decodes [source] into [outChars], recording for every decoded character (and for the end of the decoded text)
     * the offset in [source] it originates from in [sourceOffsets]. The decoded text is never longer than [source],
     * so [sourceOffsets] needs to have at least `source.length + 1` entries.
     */
    fun decodeEscapes(
      source: String,
      outChars: StringBuilder,
      sourceOffsets: IntArray,
    ) {
      val outStart = outChars.length
      var index = 0

      fun record(sourceIndex: Int) {
        sourceOffsets[outChars.length - outStart] = sourceIndex
      }

      fun appendCodePoint(codePoint: Int, sourceIndex: Int) {
        outChars.appendCodePoint(codePoint)
        if (Character.charCount(codePoint) == 2) {
          // Both chars of the surrogate pair originate from the same escape sequence.
          sourceOffsets[outChars.length - outStart - 1] = sourceIndex
        }
      }

      fun tryDecodeHex(digits: Int): Boolean {
        val start = index + 2
        val end = start + digits
        if (end > source.length) return false
        val hex = source.substring(start, end)
        if (!hex.all { it.isHexDigit() }) return false
        val codePoint = hex.toLong(16)
        if (codePoint > Character.MAX_CODE_POINT) return false
        appendCodePoint(codePoint.toInt(), index)
        index = end
        return true
      }

      fun keepBackslash() {
        // The backslash is kept literally, the following char is handled by the next iteration.
        outChars.append('\\')
        index++
      }

      while (index < source.length) {
        record(index)
        val char = source[index]
        if (char != '\\' || index + 1 >= source.length) {
          outChars.append(char)
          index++
          continue
        }
        when (val next = source[index + 1]) {
          'n' -> outChars.append('\n').also { index += 2 }
          't' -> outChars.append('\t').also { index += 2 }
          'r' -> outChars.append('\r').also { index += 2 }
          'a' -> outChars.append(BELL).also { index += 2 }
          'b' -> outChars.append(BACKSPACE).also { index += 2 }
          'f' -> outChars.append(FORM_FEED).also { index += 2 }
          'v' -> outChars.append(VERTICAL_TAB).also { index += 2 }
          '\\', '"', '\'' -> outChars.append(next).also { index += 2 }
          // A backslash followed by a newline is a line continuation: both characters are dropped.
          '\n' -> index += 2
          'x' -> if (!tryDecodeHex(2)) keepBackslash()
          'u' -> if (!tryDecodeHex(4)) keepBackslash()
          'U' -> if (!tryDecodeHex(8)) keepBackslash()
          in '0'..'7' -> {
            var end = index + 2
            while (end < source.length && end < index + 4 && source[end] in '0'..'7') end++
            appendCodePoint(source.substring(index + 1, end).toInt(8), index)
            index = end
          }
          else -> keepBackslash()
        }
      }
      record(source.length)
    }

    private fun Char.isHexDigit(): Boolean = this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    /**
     * Whether [content] can be the contents of a raw string literal with the given [quote] without changing its value.
     *
     * In a raw string, a backslash before a quote prevents the quote from terminating the literal, but both characters
     * remain part of the value. Hence, a quote that would terminate the literal cannot be represented, and neither can
     * a trailing backslash (which would escape the closing quote) or, in single-quoted literals, a line break.
     */
    fun isRepresentableAsRaw(content: String, quote: StarlarkQuote): Boolean {
      val quoteChar = quote.quote.firstOrNull() ?: return false
      if (content.endsWith('\\') || content.endsWith(quoteChar)) return false
      if (!quote.isTriple && (content.contains('\n') || content.contains('\r'))) return false
      var index = 0
      var quoteRun = 0
      while (index < content.length) {
        val char = content[index]
        if (char == '\\' && index + 1 < content.length && content[index + 1] == quoteChar) {
          index += 2
          quoteRun = 0
          continue
        }
        quoteRun = if (char == quoteChar) quoteRun + 1 else 0
        if (quoteRun == quote.quote.length) return false
        index++
      }
      return true
    }

    /** Escapes [content] so that it can be placed between the given [quote]s of a non-raw string literal. */
    fun escape(content: String, quote: StarlarkQuote): String {
      val quoteChar = quote.quote.firstOrNull()
      val result = StringBuilder(content.length)
      content.forEachIndexed { index, char ->
        when {
          char == '\\' -> result.append("\\\\")
          char == '\n' && !quote.isTriple -> result.append("\\n")
          char == '\r' && !quote.isTriple -> result.append("\\r")
          char == quoteChar && (!quote.isTriple || closesTripleQuote(content, index, quoteChar)) -> result.append('\\').append(char)
          else -> result.append(char)
        }
      }
      return result.toString()
    }

    /**
     * Inside a triple-quoted string, a quote character only needs escaping if it starts a run of three quotes
     * or if it is the last character (as it would then merge with the closing triple quote).
     */
    private fun closesTripleQuote(
      content: String,
      index: Int,
      quoteChar: Char,
    ): Boolean =
      index == content.lastIndex ||
        (index + 2 < content.length && content[index + 1] == quoteChar && content[index + 2] == quoteChar)
  }
}
