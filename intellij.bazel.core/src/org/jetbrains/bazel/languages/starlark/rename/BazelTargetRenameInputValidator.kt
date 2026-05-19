package org.jetbrains.bazel.languages.starlark.rename

import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.refactoring.rename.RenameInputValidator
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.label.LabelValidator
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression

internal class BazelTargetRenameInputValidator : RenameInputValidator {

  private val myPattern = PlatformPatterns
    .psiElement(StarlarkCallExpression::class.java)
    .with(BazelTargetRuleDeclarationPatternCondition)

  override fun getPattern(): ElementPattern<out PsiElement> = myPattern

  override fun isInputValid(
    newName: String,
    element: PsiElement,
    context: ProcessingContext,
  ): Boolean = LabelValidator.isTargetNameValid(newName)
}

private object BazelTargetRuleDeclarationPatternCondition : PatternCondition<StarlarkCallExpression>(
  "isTargetRuleDeclaration"
) {

  override fun accepts(
    element: StarlarkCallExpression,
    context: ProcessingContext?
  ): Boolean = element.isRuleTarget()
}
