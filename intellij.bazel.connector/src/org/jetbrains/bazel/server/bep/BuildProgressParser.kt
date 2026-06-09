package org.jetbrains.bazel.server.bep

import com.intellij.openapi.util.NlsContexts.ProgressDetails
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
data class BuildProgress(val fraction: Double?, @get:ProgressDetails val details: String?)

/**
 * Adapted from [com.intellij.monorepo.devkit.bazel.BazelConsoleService.Output.BuildProgressOutput]
 */
@ApiStatus.Internal
class BuildProgressParser {
  // Bazel streams progress in chunks. We surface whichever line is actually advancing:
  //  - while the "Loading:" / "Analyzing: N targets (... configured ...)" phase line keeps changing, show it;
  //  - once it freezes (analysis done, Bazel just checking cached actions) the phase line and the "[x / y]"
  //    fraction are both static, but the raw "[x / y]" counter still climbs, so show that whole line instead;
  //  - during real execution there's no phase line, so the "[x / y] N actions running" line with its fraction wins.
  private var lastPhaseLine: String? = null

  fun parse(lines: List<String>): BuildProgress? {
    var action: ActionMatch? = null
    var phaseInChunk: String? = null
    for (line in lines.asReversed()) {
      val trimmed = line.trim()
      if (action == null) {
        action = parseActionLine(trimmed)
      }
      if (phaseInChunk == null && (trimmed.startsWith("Analyzing:") || trimmed.startsWith("Loading:"))) {
        phaseInChunk = trimmed
      }
      if (action != null && phaseInChunk != null) {
        break
      }
    }

    val previousPhase = lastPhaseLine
    if (phaseInChunk != null) {
      lastPhaseLine = phaseInChunk
    }

    return when {
      phaseInChunk == null -> action?.let { BuildProgress(it.fraction, it.details) }
      phaseInChunk == previousPhase && action != null -> BuildProgress(fraction = null, details = action.line)
      else -> BuildProgress(fraction = null, details = phaseInChunk)
    }
  }

  private fun parseActionLine(trimmed: String): ActionMatch? {
    val match = progressOutputRegex.matchEntire(trimmed) ?: return null
    val total = getProgressValue(match.groups["total"]?.value, minValue = 1)
    val progress = getProgressValue(match.groups["progress"]?.value, minValue = 0).coerceAtMost(total - 1)
    return ActionMatch(trimmed, match.groups["details"]?.value, progress.toDouble() / total.toDouble())
  }

  private fun getProgressValue(text: String?, minValue: Long): Long =
    text
      ?.replace(everythingExceptDigitsRegex, "")
      ?.toLongOrNull()
      ?.coerceAtLeast(minValue)
    ?: minValue

  private data class ActionMatch(val line: String, val details: String?, val fraction: Double)

  companion object {
    private val progressOutputRegex = Regex("\\[(?<progress>[.,0-9]+)\\s*/\\s*(?<total>[.,0-9]+)]\\s*(?<details>.*)")
    private val everythingExceptDigitsRegex = Regex("[^0-9]")
  }
}
