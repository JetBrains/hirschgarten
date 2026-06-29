package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
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
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.statements.StarlarkExpressionStatement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression

internal class BazelGlobalFunctionCompletionContributor : CompletionContributor() {
  init {
    extend(CompletionType.BASIC, globalFunctionCompletionElement(), StarlarkFunctionCompletionProvider)
    extend(
      CompletionType.BASIC,
      fileSpecificFunctionCompletionElement(BazelFileType.EXTENSION),
      BazelBzlFunctionCompletionProvider,
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

  private fun fileSpecificFunctionCompletionElement(bazelFileType: BazelFileType) = globalFunctionCompletionElement()
    .inFile(
      psiFile(StarlarkFile::class.java)
        .with(bazelFileTypeCondition(bazelFileType)),
    )

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

private abstract class BazelFunctionCompletionProvider : CompletionProvider<CompletionParameters>() {

  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val project = parameters.editor.project ?: return
    if (parameters.isInStatementPosition()) {
      getFunctions(project).forEach { result.addElement(functionLookupElement(it)) }
      return
    }
    getFunctions(project)
      .filter { it.returnType != null }
      .forEach { result.addElement(functionLookupElement(it)) }
  }

  private fun CompletionParameters.isInStatementPosition(): Boolean =
    when (val grandParent = position.parent?.parent) {
      is StarlarkExpressionStatement -> true
      is StarlarkCallExpression -> grandParent.parent is StarlarkExpressionStatement
      else -> false
    }

  protected abstract fun getFunctions(project: Project): Collection<BazelGlobalFunction>

  private fun functionLookupElement(function: BazelGlobalFunction): LookupElement =
    LookupElementBuilder
      .create(function.name)
      .withInsertHandler(insertHandlerFor(function))
      .withIcon(PlatformIcons.FUNCTION_ICON)

  private fun insertHandlerFor(
    function: BazelGlobalFunction,
  ): InsertHandler<LookupElement> = when (function.name) {
    in LIST_LITERAL_COMPLETION_FUNCTIONS -> ListLiteralArgumentInsertHandler
    else -> ParenthesesInsertHandler.WITH_PARAMETERS
  }

  private companion object {

    private val LIST_LITERAL_COMPLETION_FUNCTIONS = setOf("glob", "exports_files", "licenses")
  }
}

private object ListLiteralArgumentInsertHandler : InsertHandler<LookupElement> {

  override fun handleInsert(context: InsertionContext, item: LookupElement) {
    val editor = context.editor
    val document = editor.document
    // Completion may be re-invoked over an existing call, where the parentheses are already present.
    // In that case place the caret inside them rather than inserting a second, broken `([""])`.
    if (document.charsSequence.getOrNull(context.tailOffset) == '(') {
      editor.caretModel.moveToOffset(context.tailOffset + 1)
    } else {
      document.insertString(context.tailOffset, "([\"\"])")
      editor.caretModel.moveToOffset(context.tailOffset - 3)
    }
    AutoPopupController.getInstance(context.project).scheduleAutoPopup(editor)
  }
}

private object StarlarkFunctionCompletionProvider : BazelFunctionCompletionProvider() {
  override fun getFunctions(project: Project): Collection<BazelGlobalFunction> = BazelGlobalFunctions
    .starlarkGlobalFunctions(project)
    .values
}

private object BazelBzlFunctionCompletionProvider : BazelFunctionCompletionProvider() {
  override fun getFunctions(project: Project): Collection<BazelGlobalFunction> = BazelGlobalFunctions
    .extensionGlobalFunctions(project)
    .values
}

private object BazelBuildFunctionCompletionProvider : BazelFunctionCompletionProvider() {
  override fun getFunctions(project: Project): Collection<BazelGlobalFunction> = BazelGlobalFunctions
    .buildGlobalFunctions(project)
    .values
}

private object BazelModuleFunctionCompletionProvider : BazelFunctionCompletionProvider() {
  override fun getFunctions(project: Project): Collection<BazelGlobalFunction> = BazelGlobalFunctions
    .moduleGlobalFunctions(project)
    .values
}

private object BazelWorkspaceFunctionCompletionProvider : BazelFunctionCompletionProvider() {
  override fun getFunctions(project: Project): Collection<BazelGlobalFunction> = BazelGlobalFunctions
    .moduleGlobalFunctions(project)
    .values
}
