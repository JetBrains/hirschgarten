package org.jetbrains.bazel.run.config

import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label

/**
 * Creates and parses space-separated list of target labels.
 * Supports targets containing spaces, but does not allow some relative labels.
 * Allowed label types:
 * - Absolute targets (`//package:target`, `@repo//package:target`)
 * - Relative root targets (`:target`)
 */
@ApiStatus.Internal
object TargetListParser {
  // treat only spaces before "[@repo]//" or ":" as target separators, others are spaces inside target name (it's legal)
  private val targetSeparator = Regex(" +(?=(?:@{1,2}[^@/ ]*)?//|:)")

  fun parseString(text: String): List<Label> =
    if (text.isNotBlank()) {
      text.trim().split(targetSeparator).filter { it.isNotEmpty() }.map { Label.parse(it) }
    } else {
      emptyList()
    }

  fun createString(targets: List<Label>): String =
    if (targets.isNotEmpty()) {
      targets.joinToString(" ") { it.toString() }
    } else {
      ""
    }
}
