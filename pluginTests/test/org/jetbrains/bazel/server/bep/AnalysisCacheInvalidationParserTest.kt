package org.jetbrains.bazel.server.bep

import io.kotest.matchers.shouldBe
import org.jetbrains.bsp.protocol.AnalysisCacheInvalidationCause
import org.junit.jupiter.api.Test

class AnalysisCacheInvalidationParserTest {
  // Helpers matching the two named parameters — makes the PTY/non-PTY distinction explicit in tests.
  private fun parseStderr(vararg lines: String) =
    AnalysisCacheInvalidationParser.parse(stderrLines = lines.toList())

  private fun parseStdout(vararg lines: String) =
    AnalysisCacheInvalidationParser.parse(stderrLines = emptyList(), stdoutLines = lines.toList())

  @Test
  fun `detects discard in non-PTY mode (message in stderr, stdout empty)`() {
    // Non-PTY: Bazel writes warnings to process stderr. stdoutLines is empty.
    val result = parseStderr("WARNING: Build option --compilation_mode has changed, discarding analysis cache.")

    result?.cause shouldBe AnalysisCacheInvalidationCause.BUILD_OPTIONS_CHANGED
    result?.changedOptionCount shouldBe 1
  }

  @Test
  fun `detects discard in PTY mode (message in stdout, stderr empty)`() {
    // PTY: a pseudo-terminal merges stderr into a single stream; process.errorStream is empty.
    // This test would have FAILED before the fix because the old code only scanned stderrLines.
    val result = parseStdout("WARNING: Build option --compilation_mode has changed, discarding analysis cache.")

    result?.cause shouldBe AnalysisCacheInvalidationCause.BUILD_OPTIONS_CHANGED
    result?.changedOptionCount shouldBe 1
  }

  @Test
  fun `detects discard caused by changed build options and counts them`() {
    val result = parseStderr("INFO: Build options --foo and --bar have changed, discarding analysis cache.")

    result?.cause shouldBe AnalysisCacheInvalidationCause.BUILD_OPTIONS_CHANGED
    result?.changedOptionCount shouldBe 2
  }

  @Test
  fun `detects explicit discard`() {
    val result = parseStderr("WARNING: Discarding analysis cache, as requested.")

    result?.cause shouldBe AnalysisCacheInvalidationCause.EXPLICIT_DISCARD
    result?.changedOptionCount shouldBe null
  }

  @Test
  fun `detects discard caused by memory pressure`() {
    val result = parseStderr("Memory pressure: discarding analysis cache to free up heap space.")

    result?.cause shouldBe AnalysisCacheInvalidationCause.MEMORY_PRESSURE
  }

  @Test
  fun `returns null when neither stream contains the marker`() {
    AnalysisCacheInvalidationParser.parse(
      stderrLines = listOf("INFO: Analyzed 3 targets (0 packages loaded, 0 targets configured)."),
      stdoutLines = emptyList(),
    ) shouldBe null
  }

  @Test
  fun `detects discard even when the message is wrapped in ANSI color codes`() {
    val esc = 27.toChar()
    val colored =
      "$esc[32mINFO:$esc[0m Build option $esc[1m--foo$esc[0m has changed, discarding $esc[1manalysis$esc[0m cache."

    parseStderr(colored)?.cause shouldBe AnalysisCacheInvalidationCause.BUILD_OPTIONS_CHANGED
  }

  @Test
  fun `detects discard in PTY stdout where CR separates overwritten progress fragments`() {
    // In PTY mode (--curses=true), Bazel overwrites progress lines using \r within a single stdout chunk.
    val ptyChunk = "\rINFO: 1,234 actions...\rWARNING: Build option --compilation_mode has changed, discarding analysis cache."

    parseStdout(ptyChunk)?.cause shouldBe AnalysisCacheInvalidationCause.BUILD_OPTIONS_CHANGED
  }

  @Test
  fun `returns the first discard when scanning many lines`() {
    val result = AnalysisCacheInvalidationParser.parse(
      stderrLines = listOf(
        "INFO: Analyzing...",
        "WARNING: Build option --compilation_mode has changed, discarding analysis cache.",
        "INFO: Build completed.",
      ),
    )

    result?.cause shouldBe AnalysisCacheInvalidationCause.BUILD_OPTIONS_CHANGED
    result?.changedOptionCount shouldBe 1
  }
}
