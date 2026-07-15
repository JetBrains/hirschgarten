package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.GAZELLE_TARGET_KEY
import org.jetbrains.bazel.languages.projectview.ScalarSection
import org.jetbrains.bazel.languages.projectview.completion.TargetCompletionProvider

internal class GazelleTargetSection : ScalarSection<Label?>() {
  override val sectionKey = GAZELLE_TARGET_KEY
  override val completionProvider = TargetCompletionProvider()

  override fun fromRawValue(rawValue: String): Label = Label.parse(rawValue)
}
