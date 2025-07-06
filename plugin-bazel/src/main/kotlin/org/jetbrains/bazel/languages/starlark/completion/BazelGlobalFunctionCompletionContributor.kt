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
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression

class BazelGlobalFunctionCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, globalFunctionCompletionElement(), StarlarkFunctionCompletionProvider)
    extend(
      CompletionType.BASIC,
      fileSpecificFunctionCompletionElement(BazelFileType.EXTENSION),
      BazelExtensionFunctionCompletionProvider,
    )
    extend(
      CompletionType.BASIC,
      fileSpecificFunctionCompletionElement(BazelFileType.BUILD),
      BazelBuildFunctionCompletionProvider,
    )
    extend(
      CompletionType.BASIC,
      fileSpecificFunctionCompletionElement(BazelFileType.MODULE),
      BazelModuleFunctionCompletionProvider,
    )
    extend(
      CompletionType.BASIC,
      fileSpecificFunctionCompletionElement(BazelFileType.WORKSPACE),
      BazelWorkspaceFunctionCompletionProvider,
    )
  }

  private fun fileSpecificFunctionCompletionElement(bazelFileType: BazelFileType) =
    globalFunctionCompletionElement()
      .inFile(psiFile(StarlarkFile::class.java).with(bazelFileTypeCondition(bazelFileType)))

  private fun globalFunctionCompletionElement() =
    psiElement()
      .withLanguage(StarlarkLanguage)
      .withParent(StarlarkReferenceExpression::class.java)
      .andNot(psiComment())
      .andNot(psiElement().afterLeaf(psiElement(StarlarkTokenTypes.DOT)))

  private fun bazelFileTypeCondition(bazelFileType: BazelFileType) =
    object : PatternCondition<StarlarkFile>("withBazelFileType") {
      override fun accepts(file: StarlarkFile, context: ProcessingContext): Boolean = file.getBazelFileType() == bazelFileType
    }
}

private abstract class BazelFunctionCompletionProvider(val functionNames: Set<String>) : CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    functionNames.forEach { result.addElement(functionLookupElement(it)) }
  }

  private fun functionLookupElement(name: String): LookupElement =
    LookupElementBuilder
      .create(name)
      .withInsertHandler(ParenthesesInsertHandler.NO_PARAMETERS)
      .withIcon(PlatformIcons.FUNCTION_ICON)
}

private object StarlarkFunctionCompletionProvider :
  BazelFunctionCompletionProvider(BazelGlobalFunctions.STARLARK_FUNCTIONS.keys)

private object BazelExtensionFunctionCompletionProvider :
  BazelFunctionCompletionProvider(BazelGlobalFunctions.EXTENSION_FUNCTIONS.keys)

private object BazelBuildFunctionCompletionProvider :
  BazelFunctionCompletionProvider(BazelGlobalFunctions.BUILD_FUNCTIONS.keys)

private object BazelModuleFunctionCompletionProvider :
  BazelFunctionCompletionProvider(BazelGlobalFunctions.MODULE_FUNCTIONS.keys)

private object BazelWorkspaceFunctionCompletionProvider :
  BazelFunctionCompletionProvider(BazelGlobalFunctions.WORKSPACE_FUNCTIONS.keys)
