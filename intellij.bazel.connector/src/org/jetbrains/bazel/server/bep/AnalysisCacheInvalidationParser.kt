package org.jetbrains.bazel.server.bep

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.AnalysisCacheInvalidation
import org.jetbrains.bsp.protocol.AnalysisCacheInvalidationCause

/**
 * Detects a discard of Bazel's in-server (Skyframe) analysis cache from a Bazel invocation's stderr.
 *
 * Bazel prints `... discarding analysis cache` (e.g. `WARNING: Build option --X has changed, discarding analysis
 * cache.`) to its process stderr when it drops the cache, forcing a full re-analysis. This is parsed from the bazel
 * client's stderr rather than from BEP progress events, since that is the authoritative capture of console output.
 */
@ApiStatus.Internal
object AnalysisCacheInvalidationParser {
  private const val DISCARD_MARKER = "discarding analysis cache"
  private val optionNamePattern = "--[\\w-]+".toRegex()

  /**
   * Returns the first analysis-cache discard detected in [stderrLines] or [stdoutLines], or `null` if none found.
   *
   * Both sources are needed because the capture stream depends on the process mode:
   * - Non-PTY (`--curses=false`): Bazel writes warnings to its process stderr → [stderrLines] has the message.
   * - PTY (`--curses=true`, e.g. when a terminal is attached): the OS merges stdout and stderr into one PTY stream;
   *   `process.errorStream` is empty and all output goes to `process.inputStream` → [stdoutLines] has the message.
   *
   * In PTY mode, `\r` is used to overwrite progress fragments within a single stdout chunk; it is treated as
   * whitespace before the marker check.
   */
  fun parse(stderrLines: List<String>, stdoutLines: List<String> = emptyList()): AnalysisCacheInvalidation? {
    val lines = stderrLines + stdoutLines
    for (raw in lines) {
      // Strip ANSI color/cursor codes (always present with --color=true, sometimes with cursor-control in PTY mode).
      // In PTY mode, \r separates overwritten progress fragments within a single line — treat them as whitespace.
      val line = AnsiEscapeCodes.strip(raw).replace('\r', ' ')
      val lower = line.lowercase()
      if (!lower.contains(DISCARD_MARKER)) continue

      val cause = when {
        lower.contains("memory") -> AnalysisCacheInvalidationCause.MEMORY_PRESSURE
        lower.contains("discard_analysis_cache") || lower.contains("as requested") ->
          AnalysisCacheInvalidationCause.EXPLICIT_DISCARD
        lower.contains("option") -> AnalysisCacheInvalidationCause.BUILD_OPTIONS_CHANGED
        else -> AnalysisCacheInvalidationCause.UNKNOWN
      }
      val changedOptionCount =
        if (cause == AnalysisCacheInvalidationCause.BUILD_OPTIONS_CHANGED) {
          optionNamePattern.findAll(line).count().takeIf { it > 0 }
        }
        else {
          null
        }
      return AnalysisCacheInvalidation(cause, changedOptionCount)
    }
    return null
  }
}
