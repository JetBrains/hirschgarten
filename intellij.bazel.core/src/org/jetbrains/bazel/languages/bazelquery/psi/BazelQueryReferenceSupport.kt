package org.jetbrains.bazel.languages.bazelquery.psi

import com.intellij.model.psi.PsiSymbolReference
import org.jetbrains.annotations.ApiStatus

/**
 * Builds references for bazelquery PSI elements.
 * The PSI has no references when no implementation is registered.
 */
@ApiStatus.Internal
interface BazelQueryReferenceSupport {
  fun getOwnReferences(element: BazelQueryCommand): Collection<PsiSymbolReference>

  fun getOwnReferences(element: BazelQueryFlag): Collection<PsiSymbolReference>
}
