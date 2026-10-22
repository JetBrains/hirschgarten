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

  /**
   * Tells if the message or the stack trace names a package or the plugin id of the Bazel plugin.
   * A frame of the plugin, a cause with such a frame, and a coroutine of the plugin scope all name it.
   */
  fun namesBazelPlugin(): Boolean = BAZEL_PLUGIN_NAME.containsMatchIn(message.orEmpty()) || BAZEL_PLUGIN_NAME.containsMatchIn(throwableText)
}

private val BAZEL_PLUGIN_NAME = Regex("""\b(org\.jetbrains\.(bazel|bsp)|com\.intellij\.bazel)\b""")

/**
 * Returns the failure message for the [errors] that the IDE logged up to the end of [phase], or null when no error fails the phase.
 *
 * Only an error that [names the Bazel plugin][IdeErrorText.namesBazelPlugin] fails the phase.
 * [otherFatalIdeErrorsReport] lists the other errors.
 */
internal fun fatalIdeErrorsFailure(phase: String, errors: List<IdeErrorText>): String? {
  val bazelErrors = errors.filter { !it.isShortUiFreeze() && it.namesBazelPlugin() }
  if (bazelErrors.isEmpty()) return null
  return "Fatal IDE errors detected during $phase:\n\n${renderErrors(bazelErrors)}"
}

/**
 * Returns a report of the [errors] that do not fail [phase], or null when there are none.
 * The IDE Starter reports each of these errors as a test of its own after the IDE closes.
 */
internal fun otherFatalIdeErrorsReport(phase: String, errors: List<IdeErrorText>): String? {
  val otherErrors = errors.filter { !it.isShortUiFreeze() && !it.namesBazelPlugin() }
  if (otherErrors.isEmpty()) return null
  return "IDE errors that do not name the Bazel plugin, up to the end of $phase. They do not fail the test here:\n\n${renderErrors(otherErrors)}"
}

private fun renderErrors(errors: List<IdeErrorText>): String {
  val renderedErrors = errors.take(3).joinToString("\n\n---\n\n") { it.render() }
  val remainingErrors = errors.size - 3
  val suffix = if (remainingErrors > 0) "\n\n... and $remainingErrors more fatal IDE error(s)" else ""
  return renderedErrors + suffix
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
