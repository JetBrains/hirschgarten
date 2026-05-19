package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.psi.PsiElement
import org.jetbrains.bazel.languages.starlark.psi.expressions.isRuleTarget

internal class BazelTargetDocumentationProvider : AbstractDocumentationProvider() {

  @Suppress("HardCodedStringLiteral")
  override fun getQuickNavigateInfo(element: PsiElement, originalElement: PsiElement?): String? {
    if (!element.isRuleTarget()) return null
    return "'${element.getCalledFunctionName()}' target"
  }
}
