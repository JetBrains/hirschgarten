package org.jetbrains.bazel.languages.bazelrc.psi

import com.intellij.lang.ASTNode
import com.intellij.model.psi.PsiSymbolReference
import com.intellij.openapi.components.serviceOrNull
import com.intellij.psi.PsiElement
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bazel.languages.bazelrc.elements.BazelrcTokenTypes

@ApiStatus.Internal
class BazelrcFlag(node: ASTNode) : BazelrcBaseElement(node) {
  override fun acceptVisitor(visitor: BazelrcElementVisitor) = visitor.visitFlag(this)

  val name: PsiElement?
    get() = findChildByType(BazelrcTokenTypes.FLAG)

  override fun getOwnReferences(): Collection<PsiSymbolReference> =
    serviceOrNull<BazelrcReferenceSupport>()?.getOwnReferences(this) ?: emptyList()
}
