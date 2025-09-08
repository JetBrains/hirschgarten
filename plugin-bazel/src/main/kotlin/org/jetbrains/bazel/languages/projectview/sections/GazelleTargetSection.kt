package org.jetbrains.bazel.languages.projectview.sections

import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.completion.TargetCompletionProvider
import org.jetbrains.bazel.languages.projectview.ScalarSection
import org.jetbrains.bazel.languages.projectview.SectionKey

class GazelleTargetSection : ScalarSection<Label>() {
  override val name = NAME
  override val sectionKey = KEY
  override val completionProvider = TargetCompletionProvider()

  override fun fromRawValue(rawValue: String): Label = Label.parse(rawValue)

  companion object {
    const val NAME = "gazelle_target"
    val KEY = SectionKey<Label>(NAME)
  }
}
