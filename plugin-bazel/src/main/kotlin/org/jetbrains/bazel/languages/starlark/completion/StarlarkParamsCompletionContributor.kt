package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkParameterList

class StarlarkParamsCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, paramElement("*"), StarlarkParamsCompletionProvider("args"))
    extend(CompletionType.BASIC, paramElement("**"), StarlarkParamsCompletionProvider("kwargs"))
  }
}

private class StarlarkParamsCompletionProvider(val lookupString: String) : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet
  ) {
    result.addElement(LookupElementBuilder.create(lookupString))
  }
}

private fun paramElement(token: String) =
  psiElement().inside(StarlarkParameterList::class.java).afterLeaf(token)
