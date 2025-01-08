package org.jetbrains.bazel.languages.starlark.completion

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.languages.starlark.completion.lookups.StarlarkNamedLookupElement
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
  override fun addCompletions(
    parameters: CompletionParameters,
    context: ProcessingContext,
    result: CompletionResultSet,
  ) {
    val argument = PsiTreeUtil.getParentOfType(parameters.position, StarlarkArgumentExpression::class.java) ?: return
    argument.reference.variants
      .filterIsInstance<StarlarkNamedLookupElement>()
      .forEach { result.addElement(it) }
  }
}
