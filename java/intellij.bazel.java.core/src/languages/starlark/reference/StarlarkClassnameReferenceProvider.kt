package org.jetbrains.bazel.languages.starlark.reference

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceProvider
import com.intellij.util.ProcessingContext
import org.jetbrains.bazel.languages.starlark.bazel.StarlarkClassParametersProvider
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression
import org.jetbrains.bazel.languages.starlark.references.StarlarkClassnameReference

internal object StarlarkClassnameReferenceProvider : PsiReferenceProvider() {
  private fun classParametersList(): Set<String> =
    StarlarkClassParametersProvider.EP_NAME.extensionList
      .flatMap { it.getClassnameParameters() }
      .toSet()

  override fun getReferencesByElement(
    element: PsiElement,
    context: ProcessingContext,
  ): Array<out PsiReference?> {
    if (element is StarlarkStringLiteralExpression && isClassnameValue(element)) {
      return arrayOf(StarlarkClassnameReference(element))
    }
    return arrayOf()
  }

  fun isClassnameValue(element: PsiElement): Boolean = (element.parent as? StarlarkNamedArgumentExpression)?.name in classParametersList()
}
