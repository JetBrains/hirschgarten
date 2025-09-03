package org.jetbrains.bazel.languages.projectview.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.util.startOffset
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.languages.projectview.lexer.ProjectViewTokenType
import org.jetbrains.bazel.target.targetUtils

class TargetCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val project = parameters.position.project
    var prefix =
      parameters.position.text
        .take(parameters.offset - parameters.position.startOffset)
        .removePrefix("-")

    if (parameters.position.prevSibling
        ?.node
        ?.elementType == ProjectViewTokenType.COLON
    ) {
      prefix = ":$prefix"
    }

    result.withPrefixMatcher(ProjectViewPrefixMatcher(prefix)).run {
      addAllElements(project.targetUtils.allTargetsAndLibrariesLabels.map { labelLookupElement(it) })
    }
  }

  private fun labelLookupElement(label: String): LookupElement =
    LookupElementBuilder
      .create(label)
      .withIcon(PlatformIcons.PACKAGE_ICON)
}
