package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.patterns.PlatformPatterns.psiComment
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.bazel.BazelNativeRules
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression


class BazelTargetsCompletionContributor : CompletionContributor() {
  init {
    extend(
      CompletionType.BASIC,
      element(),
      Provider(),
    )
  }
  private fun element() =
    psiElement()
      .withLanguage(StarlarkLanguage)


  private class Provider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
      parameters: CompletionParameters,
      context: ProcessingContext,
      result: CompletionResultSet,
    ) {
      result.addElement(LookupElementBuilder.create("Hello"))
    }
  }
}
