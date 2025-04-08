package org.jetbrains.bazel.languages.starlark.references

import com.intellij.openapi.util.TextRange
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.bazel.config.isBazelProject
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression

class StarlarkClassnameReference(element: StarlarkStringLiteralExpression) :
  PsiReferenceBase<StarlarkStringLiteralExpression>(element, TextRange(0, element.textLength), false) {
  override fun resolve(): PsiElement? {
    val project = element.project
    if (!project.isBazelProject) return null

    val scope = GlobalSearchScope.allScope(project)
    // JavaPsiFacade works here both for Java and Kotlin. It is tested in
    // org.jetbrains.bazel.languages.starlark.references.StarlarkClassnameReferenceTest.
    return JavaPsiFacade.getInstance(project).findClass(element.getStringContents(), scope)
  }
}
