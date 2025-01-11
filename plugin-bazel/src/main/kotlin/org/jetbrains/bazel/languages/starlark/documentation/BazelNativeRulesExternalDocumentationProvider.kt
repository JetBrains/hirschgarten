package org.jetbrains.bazel.languages.starlark.documentation

import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiElement


class BazelNativeRulesExternalDocumentationProvider : PsiDocumentationTargetProvider {
  override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
    return super.documentationTarget(element, originalElement)
  }
}
