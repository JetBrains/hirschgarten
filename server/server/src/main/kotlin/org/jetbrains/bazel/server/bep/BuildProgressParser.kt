package org.jetbrains.bazel.server.bep

data class BuildProgress(val fraction: Double, val details: String?)

/**
 * Adapted from [com.intellij.monorepo.devkit.bazel.BazelConsoleService.Output.BuildProgressOutput]
 */
class BuildProgressParser {
  fun parse(lines: List<String>): BuildProgress? {
    for (line in lines.reversed()) {
      val trimmed = line.trim()
      progressOutputRegex.matchEntire(trimmed)?.let { matchResult ->
        val total = getProgressValue(matchResult.groups["total"]?.value, minValue = 1)
        val progress = getProgressValue(matchResult.groups["progress"]?.value, minValue = 0)
          .coerceAtMost(total - 1)

        return BuildProgress(progress.toDouble() / total.toDouble(), matchResult.groups["details"]?.value)
      }
    }
    return null
  }

  private fun getProgressValue(text: String?, minValue: Long): Long =
    text
      ?.replace(everythingExceptDigitsRegex, "")
      ?.toLongOrNull()
      ?.coerceAtLeast(minValue)
    ?: minValue

  companion object {
    private val progressOutputRegex = Regex("\\[(?<progress>[.,0-9]+)\\s*/\\s*(?<total>[.,0-9]+)]\\s*(?<details>.*)")
    private val everythingExceptDigitsRegex = Regex("[^0-9]")
  }
}
