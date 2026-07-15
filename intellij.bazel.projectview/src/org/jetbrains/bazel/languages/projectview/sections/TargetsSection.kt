package org.jetbrains.bazel.languages.projectview.sections

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.label.Label
import org.jetbrains.bazel.languages.projectview.TARGETS_KEY
import org.jetbrains.bazel.languages.projectview.completion.TargetCompletionProvider
import org.jetbrains.bazel.languages.projectview.sections.presets.ExcludableListSection

@ApiStatus.Internal
class TargetsSection : ExcludableListSection<Label>() {
  override val sectionKey = TARGETS_KEY
  override val completionProvider: CompletionProvider<CompletionParameters> = TargetCompletionProvider()
  override val doc =
    "A list of bazel target expressions. To resolve source files under an imported directory, the source must be " +
      "reachable from one of your targets. Because these are full bazel target expressions, they support /... notation."

  override fun parseItem(value: String): Label? = Label.parseOrNull(value)
}
