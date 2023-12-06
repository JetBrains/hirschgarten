package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiComment
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.languages.starlark.StarlarkBuiltIn
import org.jetbrains.bazel.languages.starlark.StarlarkBundle
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.impl.StarlarkReferenceExpressionImpl

class StarlarkBuiltInCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, nameCompletionElement, StarlarkBuiltInConstCompletionProvider)
    extend(CompletionType.BASIC, nameCompletionElement, StarlarkBuiltInFunctionCompletionProvider)
    extend(CompletionType.BASIC, stringCompletionElement, StarlarkBuiltInStringCompletionProvider)
  }
}

private object StarlarkBuiltInConstCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet
  ) {
    StarlarkBuiltIn.CONSTS.forEach { result.addElement(builtInLookupElement(it)) }
  }
}

private object StarlarkBuiltInFunctionCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet
  ) {
    StarlarkBuiltIn.FUNCTIONS.forEach { result.addElement(functionLookupElement(it)) }
  }
}

private object StarlarkBuiltInStringCompletionProvider : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet
  ) {
    StarlarkBuiltIn.STRING_METHODS.forEach { result.addElement(functionLookupElement(it)) }
  }
}

private fun functionLookupElement(name: String) =
  builtInLookupElement(name).withInsertHandler(ParenthesesInsertHandler.WITH_PARAMETERS)

private fun builtInLookupElement(name: String) =
  LookupElementBuilder.create(name).withTypeText(StarlarkBundle.message("completion.builtin"))

private val nameCompletionElement = psiElement()
  .withLanguage(StarlarkLanguage)
  .withParent(StarlarkReferenceExpressionImpl::class.java)
  .andNot(psiComment())
  .andNot(psiElement().afterLeaf(psiElement(StarlarkTokenTypes.DOT)))

private val stringCompletionElement = psiElement()
  .withLanguage(StarlarkLanguage)
  .withParent(StarlarkReferenceExpressionImpl::class.java)
  .afterLeaf(psiElement(StarlarkTokenTypes.DOT).afterLeaf(psiElement(StarlarkTokenTypes.STRING)))
