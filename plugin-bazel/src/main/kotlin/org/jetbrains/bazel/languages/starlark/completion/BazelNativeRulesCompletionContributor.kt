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
import com.intellij.openapi.project.Project
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns.psiComment
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PlatformPatterns.psiFile
import com.intellij.util.PlatformIcons
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.languages.starlark.StarlarkLanguage
import org.jetbrains.bazel.languages.starlark.bazel.BazelFileType
import org.jetbrains.bazel.languages.starlark.bazel.BazelNativeRule
import org.jetbrains.bazel.languages.starlark.bazel.BazelNativeRules
import org.jetbrains.bazel.languages.starlark.documentation.BazelNativeRuleDocumentationSymbol
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
      BazelNativeRules.NATIVE_RULES_MAP.values.forEach { result.addElement(functionLookupElement(it, parameters.position.project)) }
    }

    private class NativeRuleInsertHandler<T : LookupElement>(val rule: BazelNativeRule) : InsertHandler<T> {
      override fun handleInsert(context: InsertionContext, item: T) {
        val editor = context.editor
        val document = editor.document
        document.insertString(context.tailOffset, "(\n")

        val requiredArgs = rule.arguments.filter { it.required }
        requiredArgs.forEach {
          document.insertString(context.tailOffset, "\t${it.name} = ${it.default},\n")
        }

        document.insertString(context.tailOffset, "\t\n)")
        editor.caretModel.moveToOffset(context.tailOffset - 2)
      }
    }

    private fun functionLookupElement(rule: BazelNativeRule, project: Project): LookupElement =
      LookupElementBuilder
        .create(BazelNativeRuleDocumentationSymbol(rule, project), rule.name)
        .withIcon(PlatformIcons.FUNCTION_ICON)
        .withInsertHandler(NativeRuleInsertHandler(rule))
  }
}
