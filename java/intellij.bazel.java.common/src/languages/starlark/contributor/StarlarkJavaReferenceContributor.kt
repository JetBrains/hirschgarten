package org.jetbrains.bazel.languages.starlark.contributor

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceRegistrar
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.reference.StarlarkClassnameReferenceProvider

internal class StarlarkJavaReferenceContributor : PsiReferenceContributor() {
  override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
    registrar.registerReferenceProvider(
      PlatformPatterns.psiElement(StarlarkStringLiteralExpression::class.java),
      StarlarkClassnameReferenceProvider,
    )
  }
}
