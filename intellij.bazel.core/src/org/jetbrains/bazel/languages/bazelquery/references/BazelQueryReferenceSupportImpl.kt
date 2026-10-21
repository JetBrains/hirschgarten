package org.jetbrains.bazel.languages.bazelquery.references

import com.intellij.model.psi.PsiSymbolReference
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryCommand
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryFlag
import org.jetbrains.bazel.languages.bazelquery.psi.BazelQueryReferenceSupport

@Suppress("UnstableApiUsage")
internal class BazelQueryReferenceSupportImpl : BazelQueryReferenceSupport {
  override fun getOwnReferences(element: BazelQueryCommand): Collection<PsiSymbolReference> =
    element.name?.let { listOf(BazelQueryFunctionReference(element)) } ?: emptyList()

  override fun getOwnReferences(element: BazelQueryFlag): Collection<PsiSymbolReference> =
    element.name?.let { listOf(BazelQueryFlagNameReference(element)) } ?: emptyList()
}
