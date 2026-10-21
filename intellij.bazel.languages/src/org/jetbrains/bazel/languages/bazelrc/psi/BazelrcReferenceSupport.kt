package org.jetbrains.bazel.languages.bazelrc.psi

import com.intellij.model.psi.PsiSymbolReference
import com.intellij.psi.PsiReference
import org.jetbrains.annotations.ApiStatus

/**
 * Builds references for bazelrc PSI elements.
 * The PSI has no references when no implementation is registered.
 */
@ApiStatus.Internal
interface BazelrcReferenceSupport {
  fun getOwnReferences(element: BazelrcFlag): Collection<PsiSymbolReference>

  fun getReference(element: BazelrcImport): PsiReference?

  fun getReferences(element: BazelrcImport): Array<out PsiReference?>
}
