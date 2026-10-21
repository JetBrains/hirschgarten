package org.jetbrains.bazel.languages.bazelrc.references

import com.intellij.model.psi.PsiSymbolReference
import com.intellij.psi.PsiReference
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcFlag
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcImport
import org.jetbrains.bazel.languages.bazelrc.psi.BazelrcReferenceSupport

@Suppress("UnstableApiUsage")
internal class BazelrcReferenceSupportImpl : BazelrcReferenceSupport {
  override fun getOwnReferences(element: BazelrcFlag): Collection<PsiSymbolReference> =
    element.name?.let { listOf(BazelrcFlagNameReference(element)) } ?: emptyList()

  override fun getReference(element: BazelrcImport): PsiReference? = element.references.last()

  override fun getReferences(element: BazelrcImport): Array<out PsiReference?> = BazelrcImportFileReferenceSet(element).allReferences
}
