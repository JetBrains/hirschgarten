package org.jetbrains.bazel.languages.starlark.findusages

import com.intellij.codeInsight.TargetElementEvaluatorEx2
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.psi.StarlarkNamedElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression

/**
 * Decides which named Starlark elements are targets of find usages and refactorings.
 * A [StarlarkCallExpression] is a target only when it declares a rule target. A name is a target only when it binds.
 * Without this, every call and every read would count as "named".
 */
internal class BazelRuleTargetCallElementEvaluator : TargetElementEvaluatorEx2() {

  override fun isAcceptableNamedParent(parent: PsiElement): Boolean = when (parent) {
    is StarlarkCallExpression -> parent.isRuleTarget()
    // A name that only reads a variable is no declaration. An unresolved read has nothing to rename or to search.
    is StarlarkNamedElement -> parent.nameIdentifier != null
    else -> true
  }
}
