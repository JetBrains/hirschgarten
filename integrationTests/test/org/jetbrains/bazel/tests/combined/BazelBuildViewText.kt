package org.jetbrains.bazel.tests.combined

/**
 * Checks the text of the Bazel build view, as `getAllTexts().asString()` joins it.
 * The joined text has no separator between two console lines.
 * Bazel can pad an overwritten console line with spaces, so both checks treat a run of whitespace as one space.
 */
internal object BazelBuildViewText {
  private val WHITESPACE = Regex("\\s+")
  private val BUILD_END = Regex("""Build completed successfully, \d+ total actions? ?INFO: Build Event Protocol""")

  /**
   * Returns true when the build view shows the last two lines of a successful build with the escape sequences interpreted.
   *
   * Bazel colors each `INFO:` prefix with an escape sequence.
   * The two lines match only when the build view interprets that sequence, because the check allows only whitespace between them.
   * The check reads the last lines, because the earlier output can scroll out of the visible build view.
   * Without a PTY, Bazel writes no escape sequences and this check also returns true. [hasEmptyInfoLine] detects that case.
   */
  fun hasInterpretedBuildEnd(text: String): Boolean = BUILD_END.containsMatchIn(normalize(text))

  /** Returns true when the build view keeps an empty `INFO:` line. A disabled PTY cannot overwrite that line. */
  fun hasEmptyInfoLine(text: String): Boolean = "INFO: INFO:" in normalize(text)

  private fun normalize(text: String): String = text.replace(WHITESPACE, " ")
}
