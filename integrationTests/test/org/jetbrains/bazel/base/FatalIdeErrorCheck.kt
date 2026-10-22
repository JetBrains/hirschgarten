package org.jetbrains.bazel.base

/** The text of one error in the message pool of the IDE under test. */
internal class IdeErrorText(val message: String?, val throwableText: String) {
  fun render(): String =
    sequenceOf(message, throwableText)
      .filterNotNull()
      .map(String::trim)
      .filter(String::isNotBlank)
      .distinct()
      .joinToString("\n")
}

/**
 * Returns the failure message for the [errors] that the IDE logged up to the end of [phase], or null when no error fails the phase.
 */
internal fun fatalIdeErrorsFailure(phase: String, errors: List<IdeErrorText>): String? {
  val relevantErrors = errors.filterNot { it.isShortUiFreeze() }
  if (relevantErrors.isEmpty()) return null

  val renderedErrors = relevantErrors.take(3).joinToString("\n\n---\n\n") { it.render() }
  val remainingErrors = relevantErrors.size - 3
  val suffix = if (remainingErrors > 0) "\n\n... and $remainingErrors more fatal IDE error(s)" else ""
  return "Fatal IDE errors detected during $phase:\n\n$renderedErrors$suffix"
}

private const val SHORT_UI_FREEZE_THRESHOLD_MS = 10_000

private val uiFreezeMessageRegex = Regex("""UI was frozen for (\d+)ms""")

private fun IdeErrorText.isShortUiFreeze(): Boolean {
  // PerformanceWatcher reports short UI freezes as fatal IDE errors, but they do not invalidate sync results.
  val uiFreezeDurationMs = uiFreezeMessageRegex.find(render())
    ?.groupValues
    ?.get(1)
    ?.toIntOrNull()
    ?: return false
  return uiFreezeDurationMs < SHORT_UI_FREEZE_THRESHOLD_MS
}
