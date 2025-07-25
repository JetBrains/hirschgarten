package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.findParentOfType
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.languages.bazelrc.completion.letIf
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunction
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctionParameter
import org.jetbrains.bazel.languages.starlark.bazel.BazelGlobalFunctions
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkNamedLookupElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.functions.StarlarkArgumentList

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
      BazelFileType.EXTENSION -> BazelGlobalFunctions.extensionGlobalFunctions
      BazelFileType.BUILD -> BazelGlobalFunctions.buildGlobalFunctions
      BazelFileType.MODULE -> BazelGlobalFunctions.moduleGlobalFunctions
      BazelFileType.WORKSPACE -> BazelGlobalFunctions.moduleGlobalFunctions
    } + BazelGlobalFunctions.starlarkGlobalFunctions

  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val file = parameters.originalFile as? StarlarkFile ?: return
    val starlarkCallExpression = parameters.position.findParentOfType<StarlarkCallExpression>()
    val functionName = starlarkCallExpression?.firstChild?.text ?: return
    val globalFunction = fileTypeToGlobalFunctions(file)[functionName]

    if (globalFunction != null) {
      val argumentList = (starlarkCallExpression.lastChild as StarlarkArgumentList).getArgumentNames()
      addCompletionForGlobalFunction(result, globalFunction, argumentList)
    } else {
      val argument = PsiTreeUtil.getParentOfType(parameters.position, StarlarkArgumentExpression::class.java) ?: return
      argument.reference.variants
        .filterIsInstance<StarlarkNamedLookupElement>()
        .forEach { result.addElement(it) }
    }
  }

  private fun addCompletionForGlobalFunction(
    result: CompletionResultSet,
    function: BazelGlobalFunction,
    argumentList: Set<String>,
  ) {
    val filtered =
      function.params.filter {
        !argumentList.contains(it.name)
      }

    filtered.forEach {
      result.addElement(PrioritizedLookupElement.withPriority(argumentLookupElement(it), 1.0))
    }
  }

  private class ArgumentInsertHandler<T : LookupElement>(val default: String) : InsertHandler<T> {
    override fun handleInsert(context: InsertionContext, item: T) {
      val editor = context.editor
      val document = editor.document
      if (default == "\'\'" || default == "\"\"" || default == "[]" || default == "{}") {
        document.insertString(context.tailOffset, " = $default,")
        editor.caretModel.moveToOffset(context.tailOffset - 2)
      } else {
        document.insertString(context.tailOffset, " = ,")
        editor.caretModel.moveToOffset(context.tailOffset - 1)
      }
    }
  }

  private fun argumentLookupElement(arg: BazelGlobalFunctionParameter): LookupElement =
    LookupElementBuilder
      .create(arg.name)
      .withIcon(PlatformIcons.PARAMETER_ICON)
      .letIf(arg.named) {
        it.withInsertHandler(ArgumentInsertHandler(arg.defaultValue ?: ""))
      }
}
