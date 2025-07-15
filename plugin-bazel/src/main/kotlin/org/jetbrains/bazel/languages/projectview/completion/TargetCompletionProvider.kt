package org.jetbrains.bazel.languages.projectview.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.target.targetUtils

internal class TargetCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val project = parameters.position.project
    result.addAllElements(project.targetUtils.allTargetsAndLibrariesLabels.map { labelLookupElement(it) })
  }

  private fun labelLookupElement(label: String): LookupElement =
    LookupElementBuilder
      .create(label)
      .withLookupStrings(listOf(label, "-$label"))
}
