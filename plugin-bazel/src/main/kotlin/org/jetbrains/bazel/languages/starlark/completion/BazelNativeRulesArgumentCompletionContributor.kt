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
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiComment
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PlatformPatterns.psiFile
import com.intellij.psi.util.findParentOfType
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType
import org.jetbrains.bazel.languages.starlark.bazel.BazelNativeRuleArgument
import org.jetbrains.bazel.languages.starlark.bazel.BazelNativeRules
import org.jetbrains.bazel.languages.starlark.elements.StarlarkTokenTypes
import org.jetbrains.bazel.languages.starlark.psi.StarlarkFile
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression

class BazelNativeRulesArgumentCompletionContributor : CompletionContributor() {
  init {
    extend(
      CompletionType.BASIC,
      bazelBuildElement(),
      BazelBuildNativeRulesArgumentCompletionProvider(),
    )
  }

  private fun bazelBuildElement() =
    psiElement()
      .withLanguage(StarlarkLanguage)
      .inFile(psiFile(StarlarkFile::class.java).with(bazelFileTypeCondition()))
      .andNot(psiComment())
      .andNot(psiElement().afterLeaf(psiElement(StarlarkTokenTypes.DOT)))
      .andNot(psiElement().afterLeaf(psiElement(StarlarkTokenTypes.INT)))
      .inside(psiElement(StarlarkCallExpression::class.java))
      .andOr(
        psiElement().withSuperParent(2, StarlarkCallExpression::class.java),
        psiElement()
          .andOr(
            psiElement().withSuperParent(2, StarlarkArgumentExpression::class.java),
            psiElement().withParent(StarlarkArgumentExpression::class.java),
          ).andNot(psiElement().afterLeaf(psiElement(StarlarkTokenTypes.EQ)))
          .andNot(
            psiElement()
              .afterLeaf(psiElement(StarlarkTokenTypes.IDENTIFIER)),
          ),
      )

  private fun bazelFileTypeCondition() =
    object : PatternCondition<StarlarkFile>("withBazelFileType") {
      override fun accepts(file: StarlarkFile, context: ProcessingContext): Boolean = file.getBazelFileType() == BazelFileType.BUILD
    }

  private class BazelBuildNativeRulesArgumentCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
      parameters: CompletionParameters,
      context: ProcessingContext,
      result: CompletionResultSet,
    ) {
      BazelNativeRules.COMMON_ARGUMENTS.forEach {
        result.addElement(
          LookupElementBuilder
            .create(it)
            .withIcon(PlatformIcons.PARAMETER_ICON),
        )
      }

      val starlarkCallExpression = parameters.position.findParentOfType<StarlarkCallExpression>() ?: return
      val functionName = starlarkCallExpression.firstChild.text
      val rule = BazelNativeRules.NATIVE_RULES_MAP[functionName] ?: return
      rule.arguments.forEach { result.addElement(functionLookupElement(it)) }
    }

    private class ArgumentInsertHandler<T : LookupElement>(val default: String) : InsertHandler<T> {
      override fun handleInsert(context: InsertionContext, item: T) {
        val editor = context.editor
        val document = editor.document
        document.insertString(context.tailOffset, " = $default,")
        if (default == BazelNativeRules.BAZEL_EMPTY_LIST || default == BazelNativeRules.BAZEL_EMPTY_STRING) {
          editor.caretModel.moveToOffset(context.tailOffset - 2)
        } else {
          editor.caretModel.moveToOffset(context.tailOffset - default.length - 1)
        }
      }
    }

    private fun functionLookupElement(arg: BazelNativeRuleArgument): LookupElement =
      LookupElementBuilder
        .create(arg.name)
        .withIcon(PlatformIcons.PARAMETER_ICON)
        .withInsertHandler(ArgumentInsertHandler(arg.default))
  }
}