package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.isRuleTarget

internal class BazelRuleCallPsiDocumentationTargetProvider : PsiDocumentationTargetProvider {

  override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
    if (!element.isRuleTarget()) return null
    return BazelRuleCallDocumentationTarget(element)
  }
}
