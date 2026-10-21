package org.jetbrains.bazel.languages.bazelrc.psi

import com.intellij.lang.ASTNode
import com.intellij.openapi.components.serviceOrNull
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes

@ApiStatus.Internal
class BazelrcImport(node: ASTNode) : BazelrcBaseElement(node) {
  override fun acceptVisitor(visitor: BazelrcElementVisitor) = visitor.visitImport(this)

  override fun getReference(): PsiReference? = serviceOrNull<BazelrcReferenceSupport>()?.getReference(this)

  override fun getReferences(): Array<out PsiReference?> =
    serviceOrNull<BazelrcReferenceSupport>()?.getReferences(this) ?: PsiReference.EMPTY_ARRAY

  fun getImportPath(): PsiElement? = findChildByType(BazelrcTokenTypes.VALUE)

  fun isOptional(): Boolean = findChildByType<PsiElement>(BazelrcTokenTypes.IMPORT)?.text?.startsWith("try-") == true
}
