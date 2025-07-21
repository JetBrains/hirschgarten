package org.jetbrains.bazel.languages.projectview.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext

class SimpleCompletionProvider(val variants: List<String>) : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    result.addAllElements(variants.map { sectionItemLookupElement(it) })
  }

  private fun sectionItemLookupElement(value: String): LookupElement =
    LookupElementBuilder
      .create(value)
      .withIcon(PlatformIcons.PROPERTY_ICON)
}
