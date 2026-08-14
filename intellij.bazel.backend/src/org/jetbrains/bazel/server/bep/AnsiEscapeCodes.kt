package org.jetbrains.bazel.server.bep

/** Strips ANSI color/cursor escape sequences from Bazel console output (Bazel emits them with `--color=true`). */
internal object AnsiEscapeCodes {
  private val escapeSequence = "\\u001B\\[[\\d;]*[^\\d;]".toRegex()
  private val ESC = 27.toChar() // ESC (0x1B), built from its code point to keep the source ASCII-only

  /** Removes ANSI escape sequences. Cheap: skips the regex (and its per-line Matcher) when no escape char is present. */
  fun strip(line: String): String = if (line.indexOf(ESC) < 0) line else line.replace(escapeSequence, "")
}
