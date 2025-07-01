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
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.findParentOfType
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunction
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctionParameter
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctionsService
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkNamedLookupElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression

class StarlarkArgumentCompletionContributor : CompletionContributor() {
  init {
    extend(
      CompletionType.BASIC,
      psiElement().withParents(StarlarkReferenceExpression::class.java, StarlarkArgumentExpression::class.java),
      StarlarkArgumentCompletionProvider,
    )
  }
}

private object StarlarkArgumentCompletionProvider : CompletionProvider<CompletionParameters>() {
  private fun fileTypeToGlobalFunctions(file: StarlarkFile): Map<String, BazelGlobalFunction> =
    when (file.getBazelFileType()) {
      BazelFileType.EXTENSION -> BazelGlobalFunctions.EXTENSION_FUNCTIONS
      BazelFileType.BUILD -> BazelGlobalFunctions.BUILD_FUNCTIONS
      BazelFileType.MODULE -> service<BazelGlobalFunctionsService>().getModuleFunctions()
      BazelFileType.WORKSPACE -> BazelGlobalFunctions.WORKSPACE_FUNCTIONS
    } + BazelGlobalFunctions.STARLARK_FUNCTIONS

  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val starlarkCallExpression = parameters.position.findParentOfType<StarlarkCallExpression>()
    val functionName = starlarkCallExpression?.firstChild?.text
    val file = parameters.originalFile as? StarlarkFile ?: return
    val functions = fileTypeToGlobalFunctions(file)

    if (functionName != null) {
      val function = functions[functionName]
      if (function != null) {
        return function.params.forEach {
          result.addElement(argumentLookupElement(it))
        }
      }
    }

    val argument = PsiTreeUtil.getParentOfType(parameters.position, StarlarkArgumentExpression::class.java) ?: return
    argument.reference.variants
      .filterIsInstance<StarlarkNamedLookupElement>()
      .forEach { result.addElement(it) }
  }

  private class ArgumentInsertHandler<T : LookupElement>(val default: String) : InsertHandler<T> {
    override fun handleInsert(context: InsertionContext, item: T) {
      val editor = context.editor
      val document = editor.document
      document.insertString(context.tailOffset, " = $default,")
      if (default == "\'\'" || default == "\"\"") {
        editor.caretModel.moveToOffset(context.tailOffset - 2)
      } else {
        val selectionStart = context.tailOffset - default.length - 1
        val selectionEnd = selectionStart + default.length
        editor.selectionModel.setSelection(selectionStart, selectionEnd)
        editor.caretModel.moveToOffset(selectionEnd)
      }
    }
  }

  private fun argumentLookupElement(arg: BazelGlobalFunctionParameter): LookupElement =
    LookupElementBuilder
      .create(arg.name)
      .withIcon(PlatformIcons.PARAMETER_ICON)
      .withInsertHandler(ArgumentInsertHandler(arg.default))
}
