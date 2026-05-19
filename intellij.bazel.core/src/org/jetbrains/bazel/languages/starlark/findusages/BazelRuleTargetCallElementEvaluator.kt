package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.codeInsight.TargetElementEvaluatorEx2
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression

/**
 * It makes [StarlarkCallExpression] being [com.intellij.psi.PsiNameIdentifierOwner] effective only for target rule declarations.
 * Without this every call expression would be considered as "named" and it would allow all the find usages and refactoring actions.
 */
internal class BazelRuleTargetCallElementEvaluator : TargetElementEvaluatorEx2() {

  override fun isAcceptableNamedParent(parent: PsiElement): Boolean {
    return parent !is StarlarkCallExpression || parent.isRuleTarget()
  }
}
