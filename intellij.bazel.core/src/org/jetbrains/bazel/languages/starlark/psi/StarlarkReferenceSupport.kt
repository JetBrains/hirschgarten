package org.jetbrains.bazel.languages.starlark.psi

import com.intellij.model.psi.PsiSymbolReference
import com.intellij.psi.PsiReference
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkCallExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkGlobExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkReferenceExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkStringLiteralExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.StarlarkTargetExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkArgumentExpression
import org.jetbrains.bazel.languages.starlark.psi.expressions.arguments.StarlarkNamedArgumentExpression

/**
 * Builds references for Starlark PSI elements.
 * The PSI has no references when no implementation is registered.
 */
@ApiStatus.Internal
interface StarlarkReferenceSupport {
  fun getReference(element: StarlarkReferenceExpression): PsiReference?

  fun getReference(element: StarlarkTargetExpression): PsiReference

  fun getReference(element: StarlarkArgumentExpression): PsiReference

  fun getReference(element: StarlarkNamedArgumentExpression): PsiReference?

  fun getOwnReferences(element: StarlarkNamedArgumentExpression): Collection<PsiSymbolReference>

  fun getReference(element: StarlarkGlobExpression): PsiReference

  fun getReference(element: StarlarkStringLiteralExpression): PsiReference?

  fun getOwnReferences(element: StarlarkCallExpression): Collection<PsiSymbolReference>
}
