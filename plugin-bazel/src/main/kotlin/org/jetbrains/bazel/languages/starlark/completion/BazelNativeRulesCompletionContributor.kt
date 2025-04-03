package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiComment
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PlatformPatterns.psiFile
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType
import org.jetbrains.bazel.languages.starlark.bazel.BazelNativeRules
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression

class BazelNativeRulesCompletionContributor : CompletionContributor() {
  init {
    extend(
      CompletionType.BASIC,
      bazelBuildElement(),
      BazelBuildNativeRulesCompletionProvider(),
    )
  }

  private fun bazelBuildElement() =
    psiElement()
      .withLanguage(StarlarkLanguage)
      .withParent(StarlarkReferenceExpression::class.java)
      .withSuperParent(3, StarlarkFile::class.java)
      .andNot(psiComment())
      .andNot(psiElement().afterLeaf(psiElement(StarlarkTokenTypes.DOT)))
      .inFile(psiFile(StarlarkFile::class.java).with(bazelFileTypeCondition()))

  private fun bazelFileTypeCondition() =
    object : PatternCondition<StarlarkFile>("withBazelFileType") {
      override fun accepts(file: StarlarkFile, context: ProcessingContext): Boolean = file.getBazelFileType() == BazelFileType.BUILD
    }

  private class BazelBuildNativeRulesCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
      parameters: CompletionParameters,
      context: ProcessingContext,
      result: CompletionResultSet,
    ) {
      BazelNativeRules.ruleNames.forEach { result.addElement(functionLookupElement(it)) }
    }

    private fun functionLookupElement(name: String): LookupElement =
      LookupElementBuilder
        .create(name)
        .withInsertHandler(ParenthesesInsertHandler.WITH_PARAMETERS)
        .withIcon(PlatformIcons.FUNCTION_ICON)
  }
}
