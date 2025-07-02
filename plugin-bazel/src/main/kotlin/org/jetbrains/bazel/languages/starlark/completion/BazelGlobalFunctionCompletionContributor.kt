package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.components.service
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiComment
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PlatformPatterns.psiFile
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunction
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctionsService
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
      fileSpecificFunctionCompletionElement(BazelFileType.MODULE, true),
      BazelModuleFunctionCompletionProvider,
    )
    extend(
      CompletionType.BASIC,
      fileSpecificFunctionCompletionElement(BazelFileType.WORKSPACE),
      BazelWorkspaceFunctionCompletionProvider,
    )
  }

  private fun fileSpecificFunctionCompletionElement(bazelFileType: BazelFileType, topLevelOnly: Boolean = false) =
    globalFunctionCompletionElement()
      .inFile(psiFile(StarlarkFile::class.java).with(bazelFileTypeCondition(bazelFileType)))
      .let { pattern ->
        if (topLevelOnly) {
          pattern.withSuperParent(3, StarlarkFile::class.java)
        } else {
          pattern
        }
      }

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

private abstract class BazelFunctionCompletionProvider(val functions: Collection<BazelGlobalFunction>) :
  CompletionProvider<CompletionParameters>() {
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    functions.forEach { result.addElement(functionLookupElement(it)) }
  }

  private fun functionLookupElement(function: BazelGlobalFunction): LookupElement =
    LookupElementBuilder
      .create(function.name)
      .withInsertHandler(FunctionInsertHandler(function))
      .withIcon(PlatformIcons.FUNCTION_ICON)

  private class FunctionInsertHandler<T : LookupElement>(val function: BazelGlobalFunction) : InsertHandler<T> {
    override fun handleInsert(context: InsertionContext, item: T) {
      val editor = context.editor
      val document = editor.document
      document.insertString(context.tailOffset, "(\n")

      val requiredArgs = function.params.filter { it.required }
      var caretPlaced = false
      requiredArgs.forEach {
        document.insertString(context.tailOffset, "\t${it.name} = ${it.default},\n")
        if (!caretPlaced) {
          caretPlaced = true
          placeCaret(context, it.default)
        }
      }

      document.insertString(context.tailOffset, "\t\n)")
      if (!caretPlaced) {
        editor.caretModel.moveToOffset(context.tailOffset - 2)
      }
    }

    private fun placeCaret(context: InsertionContext, default: String) {
      val editor = context.editor
      if (default == "\'\'" || default == "\"\"" || default == "[]" || default == "{}") {
        editor.caretModel.moveToOffset(context.tailOffset - 3)
      } else {
        val selectionStart = context.tailOffset - default.length - 1
        val selectionEnd = selectionStart + default.length
        editor.selectionModel.setSelection(selectionStart, selectionEnd)
        editor.caretModel.moveToOffset(selectionEnd)
      }
    }
  }
}

private object StarlarkFunctionCompletionProvider :
  BazelFunctionCompletionProvider(BazelGlobalFunctions.STARLARK_FUNCTIONS.values)

private object BazelExtensionFunctionCompletionProvider :
  BazelFunctionCompletionProvider(BazelGlobalFunctions.EXTENSION_FUNCTIONS.values)

private object BazelBuildFunctionCompletionProvider :
  BazelFunctionCompletionProvider(service<BazelGlobalFunctionsService>().getBuildFunctions().values)

private object BazelModuleFunctionCompletionProvider :
  BazelFunctionCompletionProvider(service<BazelGlobalFunctionsService>().getModuleFunctions().values)

private object BazelWorkspaceFunctionCompletionProvider :
  BazelFunctionCompletionProvider(BazelGlobalFunctions.WORKSPACE_FUNCTIONS.values)
